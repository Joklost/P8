using System.Collections.Generic;
using System.Linq;
using Newtonsoft.Json;

namespace ServerBackend
{
    static class Ext
    {
        public static string ToJSON(this object instance) => JsonConvert.SerializeObject(instance);
        public static T FromJSON<T>(this string json) => JsonConvert.DeserializeObject<T>(json);

        public static void Relay(this IEnumerable<Player> players, string msg)
        {
            foreach (var player in players)
            {
                player.Wsd.SendText(msg);
            }
        }
        public static void Relay(this IEnumerable<Player> players, object data)
        {
            players.Relay(data.ToJSON());
        }
    }
}