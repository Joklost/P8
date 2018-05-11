package dk.aau.sw805f18.ar.services;


import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import dk.aau.sw805f18.ar.common.websocket.Packet;
import dk.aau.sw805f18.ar.common.websocket.WebSocketWrapper;

public class AutoGroupPositionService extends Service {
    private final IBinder mBinder = new AutoGroupPositionService.LocalBinder();
    private WebSocketWrapper mWebSocket;

    public AutoGroupPositionService() {
        sendLocation();
    }

    private void sendLocation() {
        // Agree format for data
        mWebSocket.sendPacket(new Packet(Packet.POSITION_TYPE, "lat, lon"));

    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public AutoGroupPositionService getService() {
            return AutoGroupPositionService.this;
        }
    }
}
