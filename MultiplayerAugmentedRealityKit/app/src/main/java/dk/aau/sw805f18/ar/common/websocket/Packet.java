package dk.aau.sw805f18.ar.common.websocket;

public class Packet {
    public static final String OWNER_TYPE = "owner";
    public static final String MAC_TYPE = "mac";
    public static final String NAME_TYPE = "name";
    public static final String JOIN_TYPE = "join";
    public static final String START_TYPE = "start";
    public static final String PLAYERS_TYPE = "players";
    public static final String OBJECTS_TYPE = "objects";

    public static final String TRUE = "true";

    public String Type;
    public String Data;

    public Packet(String type, String data) {
        this.Type = type;
        this.Data = data;
    }
}
