package dk.aau.sw805f18.ar.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

public class WifiP2pReceiver extends BroadcastReceiver {
    private static final String TAG = WifiP2pReceiver.class.getSimpleName();
    private final WifiP2pManager mManager;
    private final WifiP2pManager.Channel mChannel;
    private final P2pSyncService mP2pSyncService;

    public WifiP2pReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, P2pSyncService p2pSyncService) {
        mManager = manager;
        mChannel = channel;
        mP2pSyncService = p2pSyncService;
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
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    ((P2pSyncService) context).setIsWifiP2pEnabled(true);
                } else {
                    ((P2pSyncService) context).setIsWifiP2pEnabled(false);
                }
                break;
            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                // The peer list has changed! We should probably do something about
                // that.
                Log.i(TAG, WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
                break;
            case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION:
                // Connection state changed! We should probably do something about
                // that.
                Log.i(TAG, WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
                break;
            case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                // Respond to this device's wifi state changing
                Log.i(TAG, WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
                break;
        }
    }
}
