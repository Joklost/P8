package dk.aau.sw805f18.ar.common.websocket;

public class Packet {
    // TODO: Clean up packet types pls
    public static final String MAC_TYPE = "mac";
    public static final String NAME_TYPE = "name";
    public static final String START_TYPE = "start";
    public static final String PLAYERS_TYPE = "players";
    public static final String OBJECTS_TYPE = "objects";
    public static final String OWNER_TYPE = "owner";            // Response to START. DATA: group ID string
    public static final String GROUP_COMPLETED_TYPE = "group";  // Send when all group members have joined, to finalize communications. DATA: n/a
    public static final String SETGROUP_TYPE = "setgroup";
    public static final String READY_TYPE = "ready";

    public static final String JOIN_TYPE = "setgroup";              // Send when joining group. DATA: group ID string
    public static final String OK_TYPE = "ok";                  // Response to JOIN. DATA: port, master device MAC

    public static final String ERROR_TYPE = "error";            // Some error occurred serverside (fx. input group ID not found). DATA: error message

    public static final String POSITION_TYPE = "position";
    public static final String NEWGROUP_TYPE = "newgroup";
    public static final String AUTO_GROUP = "autogroup";
    public static final String RANDOM_CHEST = "randomchest";
    public static final String RANDOM_CHEST_2 = "randomchest2";
    public static final String CHOSEN_CHEST_LEADER = "chosenchestmaster";
    public static final String CHOSEN_CHEST_PEER = "chosenchestslave";

    public static final String ANCHOR_TYPE = "anchor";
    public static final String ID_TYPE = "id";
    public static final String RANDOM_CHEST_ACK = "randomchestack";

    public String Type;
    public String Data;

    public Packet(String type, String data) {
        this.Type = type;
        this.Data = data;
    }
}