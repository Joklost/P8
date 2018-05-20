package dk.aau.sw805f18.ar.models;

public class Player {
    public String Id;
    public String DisplayName;
    public int Team;
    public boolean Ready;

    public Player(String id, String displayName, int team, boolean ready) {
        Id = id;
        DisplayName = displayName;
        Team = team;
        Ready = ready;
    }
    public Player() {

    }
}
