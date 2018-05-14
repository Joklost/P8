package dk.aau.sw805f18.ar.services;


import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.concurrent.ExecutionException;

import dk.aau.sw805f18.ar.common.websocket.Packet;
import dk.aau.sw805f18.ar.common.websocket.WebSocketeer;

public class AutoGroupReceiveService extends IntentService {
    private WebSocketeer mWebSocketeer;
    private final IBinder mBinder = new AutoGroupReceiveService.LocalBinder();
    private int mCurrentGroup;


    public AutoGroupReceiveService(String name) throws ExecutionException, InterruptedException {
        super(name);
        mWebSocketeer = new WebSocketeer("http://warpapp.xyz/connect/test");

        mWebSocketeer.attachHandler(Packet.NEW_GROUP_TYPE, packet -> {
            mCurrentGroup = Integer.parseInt(packet.Data);
        });
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }

    public class LocalBinder extends Binder {
        public AutoGroupReceiveService getService() {
            return AutoGroupReceiveService.this;
        }
    }

    public int getCurrentGroup() {
        return mCurrentGroup;
    }

    public void stopService() {
    }
}
