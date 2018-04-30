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
        public string DisplayName { get; set; }
        public int Team { get; set; } = 0;
        [JsonIgnore]
        public WebSocketDialog Wsd;
    }
}