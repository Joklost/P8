using System;
using System.Collections.Concurrent;
using System.Net;
using Newtonsoft.Json;
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

            var rooms = new ConcurrentDictionary<string, CourseRoom>();
            
            server.Use(new CookieSessions(new CookieSessionSettings(TimeSpan.FromDays(14))
            {
                Excluded = { "/login" }
            }));
            
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
                
                var room = new CourseRoom
                {
                    Owner = email,
                    Id = (roomId++).ToString()
                };
                rooms[room.Id] = room;
                await res.SendString(room.Id);
            });
            
            server.WebSocket("/connect/:room", async (req, wsd) =>
            {
                if (!rooms.TryGetValue(req.Parameters["room"], out var room))
                {
                    await wsd.SendText("No such room! Please consult your leader!");
                    await wsd.Close();
                    return;
                }
                
                // The logged-in user's email. Used for identification or whatever
                var email = (string) req.GetSession().Data;
                // Function that sends current list of players in room
                EventHandler playerChangeHandler = delegate { wsd.SendText(JsonConvert.SerializeObject(room.Players)); };
                // Game leader does not count as player
                if (email != room.Owner)
                    room.AddPlayer(email);

                room.PlayerJoined += playerChangeHandler;
                room.PlayerLeft += playerChangeHandler;
                
                // When this websocket connection is closed (for any reason)
                wsd.OnClosed += delegate
                {
                    if (email != room.Owner)
                        room.RemovePlayer(email);

                    room.PlayerJoined -= playerChangeHandler;
                    room.PlayerLeft -= playerChangeHandler;
                };
            });
        }
    }
}