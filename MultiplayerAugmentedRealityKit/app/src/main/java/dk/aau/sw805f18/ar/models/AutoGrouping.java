package dk.aau.sw805f18.ar.models;

import android.location.Location;

import dk.aau.sw805f18.ar.ar.location.sensor.DeviceLocation;
import dk.aau.sw805f18.ar.common.helpers.RunnableExecutor;
import dk.aau.sw805f18.ar.common.helpers.Task;
import dk.aau.sw805f18.ar.common.websocket.Packet;
import dk.aau.sw805f18.ar.common.websocket.WebSocketeer;

public class AutoGrouping {
    private DeviceLocation mDeviceLocation;
    private WebSocketeer mWebSocket;
    private boolean mTrigger = true;
    private int mCurrentGroup = 0;

    public AutoGrouping(DeviceLocation dl, WebSocketeer socket) {
        mDeviceLocation = dl;
        mWebSocket = socket;
    }

    public void start() {
        // Thread for sending position, when auto grouping
        Task.run(() -> {
            while (mTrigger) {

                Location location = mDeviceLocation.getCurrentBestLocation();
                mWebSocket.send(new Packet(
                        Packet.POSITION_TYPE,
                        String.format("{lat: %s, lon: %s}",
                                location.getLatitude(), location.getLongitude())));

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        // Thread for receiving group data, when auto grouping
        mWebSocket.attachHandler(Packet.NEW_GROUP_TYPE, packet -> {
            mCurrentGroup = Integer.parseInt(packet.Data);
        });
    }

    public void stop() {
        mTrigger = false;
        mWebSocket.removeHandler(Packet.NEW_GROUP_TYPE);
    }

    public int getcurrentGroup() {
        return mCurrentGroup;
    }
}
