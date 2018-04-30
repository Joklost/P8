using System;

namespace ServerBackend
{
    class PlayerJoinLeaveEvent : EventArgs
    {
        public readonly string Player;
        public readonly bool Join;

        public PlayerJoinLeaveEvent(string player, bool join)
        {
            Player = player;
            Join = join;
        }
    }
}