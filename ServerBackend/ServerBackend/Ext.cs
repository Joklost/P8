using System.Collections.Generic;
using Newtonsoft.Json;

namespace ServerBackend
{
    static class Ext
    {
        public static string ToJSON(this object instance) => JsonConvert.SerializeObject(instance);
        public static T FromJSON<T>(this string json) => JsonConvert.DeserializeObject<T>(json);

 
        public static void Relay(this IEnumerable<Player> players, WsMsg data)
        {
            var json = data.ToJSON();
            foreach (var player in players)
            {
                player.Wsd.SendText(json);
            }
        }
    }
}