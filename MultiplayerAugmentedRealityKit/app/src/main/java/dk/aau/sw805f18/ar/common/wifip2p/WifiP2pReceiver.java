package dk.aau.sw805f18.ar.common.wifip2p;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import dk.aau.sw805f18.ar.services.SyncService;

public class WifiP2pReceiver extends BroadcastReceiver {
    private static final String TAG = WifiP2pReceiver.class.getSimpleName();

    private final Object mLock = new Object();

    private final SyncService mSyncService;

    public WifiP2pReceiver(SyncService syncService) {
        mSyncService = syncService;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action == null) {
            return;
        }

        switch (action) {
            case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION:
                Log.i(TAG, WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
                break;
            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                // The peer list has changed!
                mSyncService.discoverPeers();
                Log.i(TAG, WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
                break;
            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                // Connection state changed!
                Log.i(TAG, WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                Log.i(TAG, "networkInfo: " + networkInfo.toString());
                if (networkInfo.isConnected()) {
                    mSyncService.requestGroupInfo(group -> {
                        if (group != null) {
                            Log.i(TAG, String.valueOf(group.getOwner().deviceAddress));
                        }
                    });
                }
                break;
            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                // Respond to this device's wifi state changing
                Log.i(TAG, WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
                WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                mSyncService.setDeviceAddress(device.deviceAddress);
                mSyncService.setDeviceName(device.deviceName);
                break;
        }
    }
}
