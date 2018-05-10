using System;

namespace ServerBackend
{
    class ArObject
    {
        public ArObject(string model, Location location, float rotation = 0, float scale = 1)
        {
            Id = Guid.NewGuid().ToString("N");
            Model = model;
            Location = location;
            Rotation = rotation;
            Scale = scale;
        }
        public string Id { get; private set; }
        
        public string Model { get; set; }
        public float Rotation { get; set; }
        public float Scale { get; set; }
        public Location Location { get; set; }
    }
}