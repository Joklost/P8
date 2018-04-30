using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Runtime.CompilerServices;
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
            int roomId = 123;

            var rooms = new ConcurrentDictionary<string, Room>();
            rooms["test"] = new Room
            {
                Owner = "[Phone] Samsung Galaxy S7 edge",
                Id = "test"
            };

//            server.Use(new CookieSessions(new CookieSessionSettings(TimeSpan.FromDays(14))
//            {
//                Excluded = {"/login"}
//            }));

            server.Get("/", async (req, res) => { await res.SendString("oh hi mark"); });
            server.Get("/mark9000", async (req, res) => { await res.SendString("oh hi mark"); });

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
                // The logged-in user's email. Used for identification or whatever
                var email = (string) req.GetSession().Data;

                var room = new Room
                {
                    Owner = email,
                    Id = (roomId++).ToString()
                };
                rooms[room.Id] = room;
                await res.SendString(room.Id);
            });

            server.WebSocket("/connect/:room", async (req, wsd) =>
            {
                Console.WriteLine();
                if (!rooms.TryGetValue(req.Parameters["room"], out var room))
                {
                    await wsd.SendText("No such room! Please consult your leader!");
                    await wsd.Close();
                    return;
                }


                // The logged-in user's email. Used for identification or whatever
                //var email = (string) req.GetSession().Data;
                var player = room.AddPlayer(roomId++.ToString(), wsd);
                Group Group() => room.Groups[player.Team];


                void ParseWsMsg(WebSocketDialog.TextMessageEventArgs textEvent)
                {
                    var msg = textEvent.Text.FromJSON<WsMsg>();
                    Console.WriteLine($"{msg.Type} : {msg.Data}");
                    switch (msg.Type)
                    {
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
                            // Make Jonas' phone to wifi p2p group owner
                            if (player.DisplayName == "[Phone] Samsung Galaxy S7 edge")
                            {
                                wsd.SendText(new WsMsg
                                {
                                    Type = "owner",
                                    Data = "true"
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
                    room.RemovePlayer(player.Id);
                };
            });

            server.Start();
            Console.ReadLine();
        }

        private static List<Group> CreateGroups(Room room)
        {
            var teams = room.Players.Select(p => p.Team).Max();
            var list = new List<Group>(teams+1);
            foreach (var player in room.Players)
            {
                list[player.Team].Players.Add(player);
            }
            return list;
        }
    }
}