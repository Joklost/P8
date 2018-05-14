package dk.aau.sw805f18.ar.services;


import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.concurrent.ExecutionException;

import dk.aau.sw805f18.ar.ar.location.sensor.DeviceLocation;
import dk.aau.sw805f18.ar.common.websocket.Packet;
import dk.aau.sw805f18.ar.common.websocket.WebSocketeer;

public class AutoGroupPositionService extends IntentService {
    private final String ACTION_LOCATION = "AUTO_GROUP_LOCATION";
    private final String ACTION_GROUP = "AUTO_GROUP_NUMBER";
    private final IBinder mBinder = new AutoGroupPositionService.LocalBinder();
    private Location mCurrentLocation;
    private WebSocketeer mWebSocketeer;
    private boolean mLoopTrigger = true;
    private int mCurrentGroup;

    public AutoGroupPositionService(String name, DeviceLocation deviceLocation, WebSocketeer socket) throws ExecutionException, InterruptedException {
        super(name);
        mWebSocketeer = socket;
        sendLocation(deviceLocation);
        receiveGroup();
    }

    private void sendLocation(DeviceLocation deviceLocation) throws InterruptedException {
        while (mLoopTrigger) {
            mCurrentLocation = deviceLocation.getCurrentBestLocation();

            mWebSocketeer.send(new Packet(Packet.POSITION_TYPE, String.format(
                    "{lat: %s, lon: %s}",
                    mCurrentLocation.getLatitude(),
                    mCurrentLocation.getLongitude())));
        }
    }

    private void receiveGroup() {
        mWebSocketeer.attachHandler(Packet.NEW_GROUP_TYPE, packet -> mCurrentGroup = Integer.parseInt(packet.Data));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String type = intent.getStringExtra("type");
        Intent toReturn = new Intent();

        if (type.equals(ACTION_LOCATION)) {
            toReturn.setAction(ACTION_LOCATION);
            toReturn.putExtra("data", mCurrentLocation);
        } else {
            toReturn.setAction(ACTION_GROUP);
            toReturn.putExtra("data", mCurrentGroup);
        }

        sendBroadcast(toReturn);
    }

    public class LocalBinder extends Binder {
        public AutoGroupPositionService getService() {
            return AutoGroupPositionService.this;
        }
    }
}
