using System;
using System.Collections.Generic;
using System.Linq;
using Newtonsoft.Json;
using Red;

namespace ServerBackend
{
    static class Ext
    {
        public static string ToJSON(this object instance) => JsonConvert.SerializeObject(instance);
        public static T FromJSON<T>(this string json) => JsonConvert.DeserializeObject<T>(json);

 
        public static void Relay(this IEnumerable<Player> players, Packet packet)
        {
            Log("RELAY", $"{packet.Type} - {packet.Data}");
            var json = packet.ToJSON();
            foreach (var player in players)
            {
                player.Wsd.SendText(json);
            }
        }
        public static void SendPacket(Player player, Packet packet)
        {
            Log("SEND", $"{player.DisplayName}: {packet.Type} - {packet.Data}");
            player.Wsd.SendText(packet.ToJSON());
        }

        public static void Log(string tag, string msg)
        {
            Console.WriteLine($"{DateTime.Now:s}: {tag}: {msg}");
        }
    }
}