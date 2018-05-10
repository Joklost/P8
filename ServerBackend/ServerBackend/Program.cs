using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Device.Location;
using System.Linq;
using System.Net;
using Accord.MachineLearning;
using Accord.Math.Distances;
using Red;
using Red.CookieSessions;

namespace ServerBackend
{
    class Program
    {
        static void Main(string[] args)
        {
            var server = new RedHttpServer(5000);

            // Temporary "id" generator
            int roomId = 1;

            var rooms = new ConcurrentDictionary<string, Room>();
            rooms["test"] = new Room
            {
                Owner = "[Phone] Jonas' S7E",
                Id = "test",
                ArObjects =
                {
                    new ArObject("andy", new Location(57.013810, 9.988546)),
                    new ArObject("rabbit", new Location(57.014057, 9.988319)),
                    new ArObject("andy", new Location(57.013810, 9.987975), scale: 2)
                }
            };


            server.Get("/", async (req, res) => { await res.SendString("killah mark"); });
            server.Get("/mark9000",
                async (req, res) =>
                {
                    await res.SendString(
                        "oh hi mark, how goes? is all well? how is Sine? and why isn't she here? what are you even doing here? you look like you aren't happy about being here, without any runescape to play? just open it up and give it a go? because why not? jonas is also doing it? join him!");
                });

            server.Post("/login", async (req, res) =>
            {
                var form = await req.GetFormDataAsync();
                if (form["email"] == "mark" && form["password"] == "mark")
                    req.OpenSession(form["email"]);
                else
                    await res.SendStatus(HttpStatusCode.BadRequest);
            });

            server.Post("/openroom", async (req, res) =>
            {
                var owner = req.Queries["owner"];
                if (string.IsNullOrEmpty(owner))
                {
                    await res.SendString("you must specify owner in an url query: ?owner=mark",
                        status: HttpStatusCode.BadRequest);
                    return;
                }

                var room = new Room
                {
                    Owner = owner,
                    Id = (roomId++).ToString()
                };
                rooms[room.Id] = room;
                await res.SendString(room.Id);
            });

            server.WebSocket("/connect/:room", async (req, wsd) =>
            {
                var roomParam = req.Parameters["room"];
                if (!rooms.TryGetValue(roomParam, out var room))
                {
                    await wsd.SendText("No such room! Please consult your leader!");
                    await wsd.Close();
                    return;
                }

                var player = room.AddPlayer(roomId++.ToString(), wsd);
                Group Group() => room.Groups[player.Team];


                void ParseWsMsg(WebSocketDialog.TextMessageEventArgs textEvent)
                {
                    var msg = textEvent.Text.FromJSON<WsMsg>();
                    Console.WriteLine($"{player.DisplayName}: {msg.Type} - {msg.Data}");
                    switch (msg.Type)
                    {
                        case "position":
                            var loc = msg.Data.FromJSON<Location>();
                            player.Location.Latitude = loc.Lat;
                            player.Location.Longitude = loc.Lon;
                            break;
                        case "autogroup":
                            if (player.DisplayName == room.Owner)
                            {
                                room.AutoGroupingMode = msg.Data == "true";
                                room.Players.Relay(textEvent.Text);
                            }
                            break;
                        case "objects":
                            room.Players.Relay(room.ArObjects);
                            break;
                        case "completed":
                            var id = msg.Data;
                            room.ArObjects.RemoveAll(ar => ar.Id == id);
                            room.Players.Relay(room.ArObjects);
                            break;
                        case "team":
                            var teamChangeEvent = msg.Data.FromJSON<TeamChangeMsg>();
                            if (player.Id == teamChangeEvent.Id || player.Id == room.Owner)
                            {
                                room.SetPlayerTeam(teamChangeEvent.Id, teamChangeEvent.Team);
                            }

                            wsd.SendText(new WsMsg
                            {
                                Type = "mac",
                                Data = Group().LeaderMac
                            }.ToJSON());
                            break;
                        case "mac":
                            var g = Group();
                            g.LeaderMac = msg.Data;
                            var response = new WsMsg
                            {
                                Type = "mac",
                                Data = g.LeaderMac
                            }.ToJSON();
                            g.Players.Where(p => p != player).Relay(response);
                            break;
                        case "name":
                            player.DisplayName = msg.Data;
                            room.Players.Where(p => p.Id != player.Id)
                                .Relay(room.Players.Select(p => new {Name = p.DisplayName, Team = p.Team}));


                            // Make Jonas' phone to wifi p2p group owner
                            if (player.DisplayName == room.Owner)
                            {
                                wsd.SendText(new WsMsg
                                {
                                    Type = "owner",
                                    Data = "true"
                                }.ToJSON());
                            }
                            // Only for dev
                            else if (!string.IsNullOrEmpty((g = Group()).LeaderMac))
                            {
                                wsd.SendText(new WsMsg
                                {
                                    Type = "mac",
                                    Data = g.LeaderMac
                                }.ToJSON());
                            }
                            else
                            {
                                wsd.SendText(new WsMsg
                                {
                                    Type = "owner",
                                    Data = "false"
                                }.ToJSON());
                            }

                            break;
                        case "start":
                            break;
                    }
                }


                wsd.OnTextReceived += (sender, eventArgs) => { ParseWsMsg(eventArgs); };
                wsd.OnClosed += delegate // When this websocket connection is closed (for any reason)
                {
                    if (room.RemovePlayer(player.Id))
                    {
                        // rooms.Remove(roomParam, out room);
                    }
                };
            });

            server.Start();
            Console.ReadLine();
        }

        private static List<Group> CreateGroups(Room room)
        {
            var teams = room.Players.Select(p => p.Team).Max();
            var list = new List<Group>(teams + 1);
            foreach (var player in room.Players)
            {
                list[player.Team].Players.Add(player);
            }

            return list;
        }
    }
}