using System.Device.Location;
using Newtonsoft.Json;
using Red;

namespace ServerBackend
{
    class Player
    {
        public Player(string id, WebSocketDialog wsd)
        {
            Id = id;
            Wsd = wsd;
        }

        public string Id { get; set; }
        public string DisplayName { get; set; } = "[Unnamed]";
        public int Team { get; set; } = 0;
        
        [JsonIgnore]
        public GeoCoordinate Location { get; set; } = GeoCoordinate.Unknown;

        public bool Ready { get; set; } = false;

        [JsonIgnore] 
        public WebSocketDialog Wsd;
    }
}