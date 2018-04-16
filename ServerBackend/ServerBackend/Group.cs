using System.Collections.Generic;

namespace ServerBackend
{
    class Group
    {
        public string LeaderMac { get; set; }
        public List<Player> Players { get; set; } = new List<Player>();
    }
}