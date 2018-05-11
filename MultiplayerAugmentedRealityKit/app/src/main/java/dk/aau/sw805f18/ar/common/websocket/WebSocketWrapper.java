package dk.aau.sw805f18.ar.common.websocket;

import android.util.Log;

import com.google.gson.Gson;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;

public class WebSocketWrapper {
    private static final String TAG = WebSocketWrapper.class.getSimpleName();

    // Have a web socket instance for each url.
    private static final Map<String, WebSocketWrapper> sInstances = new HashMap<>();

    public static WebSocketWrapper getInstance(String url) {
        WebSocketWrapper instance = sInstances.get(url);

        if (instance == null) {
            try {
                instance = new WebSocketWrapper(url);
                sInstances.put(url, instance);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Failed to instantiate WebSocket!");
            }
        }
        return instance;
    }

    private final WebSocket mWebSocket;
    private final Gson mJson;
    private final BlockingQueue<Packet> mPacketQueue;

    private WebSocketWrapper(String url) throws ExecutionException, InterruptedException {
        mJson = new Gson();
        mPacketQueue = new ArrayBlockingQueue<>(64);

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

        // TODO: Handle all cases
        switch (p.Type) {
            case Packet.MAC_TYPE:
            case Packet.OBJECTS_TYPE:
                // fallthrough
            case Packet.OWNER_TYPE:
                mPacketQueue.offer(p);
                break;
            default:
                break;
        }
    }

    public void sendPacket(Packet pkt) {
        String s = mJson.toJson(pkt, Packet.class);
        Log.i(TAG, "SEND: " + s);
        mWebSocket.send(s);
    }

    public Packet waitPacket() {
        try {
            return mPacketQueue.take();
        } catch (InterruptedException e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }

}
