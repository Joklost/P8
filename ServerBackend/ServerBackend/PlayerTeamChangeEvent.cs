using System;

namespace ServerBackend
{
    class PlayerTeamChangeEvent : EventArgs
    {
        public readonly string Player;
        public readonly int Team;

        public PlayerTeamChangeEvent(string player, int team)
        {
            Player = player;
            Team = team;
        }
    }
}