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
        public string DisplayName { get; set; } = "Unnamed jonas";
        public int Team { get; set; } = 0;
        public GeoCoordinate Location { get; set; } = new GeoCoordinate();

        [JsonIgnore] 
        public WebSocketDialog Wsd;
    }
}