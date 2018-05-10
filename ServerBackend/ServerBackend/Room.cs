using System;
using System.Collections.Generic;
using System.Device.Location;
using System.Linq;
using System.Threading.Tasks;
using Accord.MachineLearning;
using Accord.Math;
using Accord.Math.Distances;
using Red;

namespace ServerBackend
{
    class Room
    {
        public string Owner { get; set; }
        public string Id { get; set; }

        public int MinGroups { get; set; } = 2;
        public int MaxGroups { get; set; } = 4;

        public bool AutoGroupingMode
        {
            get => _autoGroupingMode;
            set
            {
                _autoGroupingMode = value;
                if (value)
                {
                    AutoGroupLoop();
                }
                
            }
        }

        private async void AutoGroupLoop()
        {
            while (AutoGroupingMode)
            {
                var players = Players.Where(p => p.Location.Latitude == 0).ToList();
                
                KMeans kmeans = new KMeans(MaxGroups);
                var observations = players.Select(p => new[] {p.Location.Latitude, p.Location.Longitude}).ToArray();
                var clusters = kmeans.Learn(observations);
                int[] labels = clusters.Decide(observations);
                
                for (int i = 0; i < labels.Length; i++)
                {
                    var player = players[i];
                    player.Team = labels[i];
                    var msg = new WsMsg
                    {
                        Type = "newgroup",
                        Data = player.Team.ToString()
                    };
                    player.Wsd.SendText(msg.ToJSON());
                }
                
                await Task.Delay(1000);
            }
        }


        private readonly Dictionary<string, Player> _players = new Dictionary<string, Player>();
        public IEnumerable<Player> Players => _players.Values;
        public List<Group> Groups { get; set; }

        public List<ArObject> ArObjects = new List<ArObject>();
        private bool _autoGroupingMode = false;

        public Room()
        {
            Groups = new List<Group>(Enumerable.Repeat<Group>(new Group(), MaxGroups));
            
        }

        public bool SetPlayerTeam(string id, int newTeam)
        {
            
            if (newTeam >= 0 && newTeam < MaxGroups && _players.TryGetValue(id, out var player))
            {
                if (Groups.Count - 1 < newTeam)
                    
                Groups[player.Team].Players.Remove(player);
                Groups[newTeam].Players.Add(player);
                player.Team = newTeam;
                var data = new WsMsg
                {
                    Type = "team",
                    Data = new TeamChangeMsg
                    {
                        Id = id,
                        Team = newTeam
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

        public bool RemovePlayer(string id)
        {
            if (_players.Remove(id))
            {
                var group = Groups.First(g => g.Players.Any(p => p.Id == id));
                if (id == group.LeaderId)
                {   // select new leader
                    
                    
                }
                var data = new WsMsg
                {
                    Type = "players",
                    Data = Players.ToJSON()
                };
                Players.Relay(data.ToJSON());
            }

            return _players.Count == 0;

        }
    }

    class PlayerDistance : IDistance<Player>
    {
        public double Distance(Player x, Player y)
        {
            return x.Location.GetDistanceTo(y.Location);
        }
    }
}