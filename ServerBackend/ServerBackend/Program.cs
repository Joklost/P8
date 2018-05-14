using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Device.Location;
using System.Linq;
using System.Net;
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
            var roomId = 1;

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

                void ParseWsMsg(WebSocketDialog.TextMessageEventArgs textEvent)
                {
                    var msg = textEvent.Text.FromJSON<WsMsg>();
                    Console.WriteLine($"{player.DisplayName}: {msg.Type} - {msg.Data}");
                    switch (msg.Type)
                    {
                        case "position":
                            var location = msg.Data.FromJSON<Location>();
                            if (player.Location == GeoCoordinate.Unknown)
                            {
                                player.Location = new GeoCoordinate(location.Lat, location.Lon);
                            }
                            else
                            {
                                player.Location.Latitude = location.Lat;
                                player.Location.Longitude = location.Lon;
                            }
                            break;
                        case "autogroup":
                            if (player.DisplayName == room.Owner)
                            {
                                room.Players.Relay(msg);
                                room.AutoGroupingMode = msg.Data == "true";
                            }
                            break;
                        case "objects":
                            room.Players.Relay(new WsMsg
                            {
                                Type = "objects",
                                Data = room.ArObjects.ToJSON()
                            });
                            break;
                        case "ready":
                            player.Ready = true;
                            if (room.Players.All(p => p.Ready))
                            {
                                room.Players.Relay(msg);
                            }
                            break;
                        case "setgroup":
                            var teamChangeEvent = msg.Data.FromJSON<GroupChangeMsg>();
                            if (player.Id == teamChangeEvent.Id || player.Id == room.Owner)
                            {
                                if (room.SetPlayerTeam(teamChangeEvent.Id, teamChangeEvent.Team))
                                {
                                    room.Players.Relay(msg);
                                }
                            }
                            break;
                        case "mac":
                            if (player.Team == -1) 
                                break;
                            
                            var g = room.Groups[player.Team];
                            if (g.LeaderId == player.Id)
                            {
                                g.LeaderMac = msg.Data;
                                g.Players.Where(p => p != player).Relay(new WsMsg
                                {
                                    Type = "mac",
                                    Data = g.LeaderMac
                                });
                            }
                            break;
                        case "name":
                            player.DisplayName = msg.Data;
                            
                            room.Players.Where(p => p.Id != player.Id).Relay(new WsMsg
                                {
                                    Type = "player",
                                    Data = room.Players.ToJSON()
                                });
                            break;
                        case "start":
                            if (player.DisplayName == room.Owner)
                            {
                                foreach (var group in room.Groups)
                                {
                                    var leader = group.Players.First();
                                    leader.Wsd.SendText(new WsMsg
                                    {
                                        Type = "owner",
                                        Data = "true"
                                    }.ToJSON());
                                }
                            }
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