package dk.aau.sw805f18.ar.common.websocket;

import android.util.Log;

import com.google.gson.Gson;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;

public class WebSocketeerServer {
    private static final String TAG = WebSocketeerServer.class.getSimpleName();

    private AsyncHttpServer mServer;
    private List<WebSocket> mSockets;
    private final Gson mJson = new Gson();
    private final HashMap<String, BiConsumer<WebSocket, Packet>> _handlers = new HashMap<>();

    public WebSocketeerServer() {
        try {
            mServer = new AsyncHttpServer();
            mSockets = new ArrayList<>();

            mServer.websocket("/", (webSocket, request) -> {
                mSockets.add(webSocket);

                webSocket.setClosedCallback(ex -> mSockets.remove(webSocket));

                webSocket.setStringCallback(msg -> onMessage(webSocket, msg));

            });
        }
        catch (Exception e) {
            Log.i("ERRORzz", e.getMessage());
        }
    }

    private void onMessage(WebSocket socket, String s) {
        Log.i(TAG, "RECV: " + s);
        Packet msg = mJson.fromJson(s, Packet.class);
        if (_handlers.containsKey(msg.Type)) {
            BiConsumer<WebSocket, Packet> handler = _handlers.get(msg.Type);
            handler.accept(socket, msg);
        }
    }


    /**
     * @param packet The packet to send to all connected websocket clients
     */
    public void sendToAll(Packet packet) {
        String msg = mJson.toJson(packet);
        for (WebSocket socket : mSockets) {
            socket.send(msg);
        }
    }

    /**
     * @param type The type of the websocket Packet
     * @param handler The handler for the given websocket Packet type
     */
    public void attachHandler(String type, BiConsumer<WebSocket, Packet> handler) {
        _handlers.put(type, handler);
        Log.i(TAG, "ATTACHED HANDLER: " + type);
    }

    /**
     * @param type The type of the websocket Packet
     */
    public void removeHandler(String type) {
        _handlers.remove(type);
        Log.i(TAG, "REMOVED HANDLER: " + type);
    }

    public int getConnectedDevices() {
        return mSockets.size();
    }
}
