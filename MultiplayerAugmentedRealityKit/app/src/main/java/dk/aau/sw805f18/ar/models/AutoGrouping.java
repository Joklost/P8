package dk.aau.sw805f18.ar.models;

import android.location.Location;
import android.util.Log;

import dk.aau.sw805f18.ar.common.helpers.SyncServiceHelper;
import dk.aau.sw805f18.ar.common.sensor.DeviceLocation;
import dk.aau.sw805f18.ar.common.helpers.Task;
import dk.aau.sw805f18.ar.common.websocket.Packet;
import dk.aau.sw805f18.ar.common.websocket.WebSocketeer;
import dk.aau.sw805f18.ar.services.SyncService;

public class AutoGrouping {
    private DeviceLocation mDeviceLocation;
    private boolean mTrigger = false;
    private int mCurrentGroup = 0;
    private final Object mWaiter = new Object();

    public AutoGrouping(DeviceLocation dl) {
        mDeviceLocation = dl;
    }

    public void start() {
        mTrigger = true;
        // Thread for sending position, when auto grouping
        Task.run(() -> {
            while (mTrigger) {
                Location location = mDeviceLocation.getCurrentBestLocation();
                SyncServiceHelper.getInstance().getWebSocket().send(new Packet(
                        Packet.POSITION_TYPE,
                        String.format("{\"Lat\": %s, \"Lon\": %s}",
                                location.getLatitude(), location.getLongitude())));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void stop() {
        mTrigger = false;
    }

    public int getCurrentGroup() {
        return mCurrentGroup;
    }
}
