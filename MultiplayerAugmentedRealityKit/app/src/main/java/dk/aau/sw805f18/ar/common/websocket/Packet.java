package dk.aau.sw805f18.ar.common.websocket;

public class Packet {
    public static final String MAC_TYPE = "mac";
    public static final String NAME_TYPE = "name";
    public static final String START_TYPE = "start";
    public static final String PLAYERS_TYPE = "players";
    public static final String OBJECTS_TYPE = "objects";
    public static final String CREATE_TYPE = "start";            // Send when creating a group. DATA: device MAC address
    public static final String OWNER_TYPE = "owner";            // Response to START. DATA: group ID string
    public static final String GROUP_COMPLETED_TYPE = "group";  // Send when all group members have joined, to finalize communications. DATA: n/a

    public static final String JOIN_TYPE = "join";              // Send when joining group. DATA: group ID string
    public static final String OK_TYPE = "ok";                  // Response to JOIN. DATA: port, master device MAC

    public static final String ERROR_TYPE = "error";            // Some error occurred serverside (fx. input group ID not found). DATA: error message

    public String Type;
    public String Data;

    public Packet(String type, String data) {
        this.Type = type;
        this.Data = data;
    }
}