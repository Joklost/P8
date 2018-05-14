package dk.aau.sw805f18.ar.common.websocket;

import android.util.Log;

import com.google.gson.Gson;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class WebSocketeer {
    private static final String TAG = WebSocketeer.class.getSimpleName();

    private final WebSocket mWebSocket;
    private final Gson mJson = new Gson();
    private final HashMap<String, Consumer<Packet>> _handlers = new HashMap<>();

    public WebSocketeer(String url) throws ExecutionException, InterruptedException {
        Future<WebSocket> webSocketFuture = AsyncHttpClient
                .getDefaultInstance()
                .websocket(url, null, (ex, webSocket) -> {
                    if (ex != null) {
                        ex.printStackTrace();
                        return;
                    }
                    webSocket.setStringCallback(this::onMessage);
                });
        mWebSocket = webSocketFuture.get();
    }

    private void onMessage(String s) {
        Log.i(TAG, "RECV: " + s);
        Packet p = mJson.fromJson(s, Packet.class);
        if (_handlers.containsKey(p.Type)) {
            Consumer<Packet> handler = _handlers.get(p.Type);
            handler.accept(p);
        }
    }

    /**
     * @param type The type of the websocket Packet
     * @param handler The handler for the given websocket Packet type
     */
    public void attachHandler(String type, Consumer<Packet> handler) {
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

    /**
     * @param packet The packet to send
     */
    public void send(Packet packet) {
        String s = mJson.toJson(packet, Packet.class);
        Log.i(TAG, "SEND: " + s);
        mWebSocket.send(s);
    }
}
