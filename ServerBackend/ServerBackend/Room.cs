using System;
using System.Collections.Generic;
using Red;

namespace ServerBackend
{
    class Room
    {
        public string Owner { get; set; }
        public string Id { get; set; }

        public int MinGroups { get; set; } = 2;
        public int MaxGroups { get; set; } = 4;

        
        private readonly Dictionary<string, Player> _players = new Dictionary<string, Player>();
        public IEnumerable<Player> Players => _players.Values;
        public List<Group> Groups { get; set; }

        public Room()
        {
            Groups = new List<Group>(MaxGroups);
        }

        public bool SetPlayerTeam(string id, int team)
        {
            
            if (team >= 0 && team < MaxGroups && _players.TryGetValue(id, out var player))
            {
                Groups[player.Team].Players.Remove(player);
                Groups[team].Players.Add(player);
                player.Team = team;
                var data = new WsMsg
                {
                    Type = "team",
                    Data = new TeamChangeMsg
                    {
                        Id = id,
                        Team = team
                    }.ToJSON()
                };
                Players.Relay(data.ToJSON());
                return true;
            }
            else
                return false;
        }
        
        public Player AddPlayer(string id, WebSocketDialog wsd)
        {
            var p = new Player(id, wsd);
            _players.Add(id, p);
            var data = new WsMsg
            {
                Type = "players",
                Data = Players.ToJSON()
            };
            Players.Relay(data.ToJSON());
            return p;
        }

        public void RemovePlayer(string id)
        {
            if (_players.Remove(id))
            {
                var data = new WsMsg
                {
                    Type = "players",
                    Data = Players.ToJSON()
                };
                Players.Relay(data.ToJSON());
            }
                
        }
    }
}