using System.Collections.Generic;

namespace ServerBackend
{
    class Group
    {
        public string LeaderMac { get; set; }
        public string LeaderId { get; set; }
        public List<Player> Players { get; set; } = new List<Player>();
    }
}