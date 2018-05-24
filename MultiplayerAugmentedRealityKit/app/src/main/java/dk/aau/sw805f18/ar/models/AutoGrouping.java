package dk.aau.sw805f18.ar.models;

import android.location.Location;

import dk.aau.sw805f18.ar.common.helpers.SyncServiceHelper;
import dk.aau.sw805f18.ar.common.helpers.Task;
import dk.aau.sw805f18.ar.common.sensor.DeviceLocation;
import dk.aau.sw805f18.ar.common.websocket.Packet;

public class AutoGrouping {
    private final Object mWaiter = new Object();
    private DeviceLocation mDeviceLocation;
    private boolean mTrigger = false;
    private int mCurrentGroup = 0;

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
}
