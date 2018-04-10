using System;
using System.Collections.Generic;

namespace ServerBackend
{
    class CourseRoom
    {
        public string Owner { get; set; }
        public string Id { get; set; }
        
        private List<string> _players = new List<string>();
        public IReadOnlyList<string> Players => _players.AsReadOnly();

        public event EventHandler PlayerJoined;
        public event EventHandler PlayerLeft;

        public void AddPlayer(string player)
        {
            _players.Add(player);
            PlayerJoined?.Invoke(this, EventArgs.Empty);
        }


        public void RemovePlayer(string email)
        {
            if (_players.Remove(email))
                PlayerLeft?.Invoke(this, EventArgs.Empty);
                
        }
    }
}