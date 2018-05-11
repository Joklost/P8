package dk.aau.sw805f18.ar.common.wifip2p;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import dk.aau.sw805f18.ar.common.adapters.PeerListAdapter;
import dk.aau.sw805f18.ar.services.SyncService;

public class WifiP2pReceiver extends BroadcastReceiver {
    private static final String TAG = WifiP2pReceiver.class.getSimpleName();

    private final Object mLock = new Object();

    private final SyncService mSyncService;
    private List<WifiP2pDevice> mPeers = new ArrayList<>();

    public WifiP2pReceiver(SyncService syncService) {
        mSyncService = syncService;
    }

    private WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {

            // Out with the old, in with the new.
            mPeers.clear();
            mPeers.addAll(peerList.getDeviceList());

            // If an AdapterView is backed by this Data, notify it
            // of the change.  For instance, if you have a ListView of available
            // peers, trigger an update.

            if (mPeers.size() == 0) {
                Log.d(TAG, "No devices found");
            }
        }
    };

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
                    ((SyncService) context).setIsWifiP2pEnabled(true);
                } else {
                    ((SyncService) context).setIsWifiP2pEnabled(false);
                }
                break;
            case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION:
                // The peer list has changed!
                Log.i(TAG, WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

                if (mSyncService != null) {
                    mSyncService.requestPeers(peerListListener);
                }
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

                // now that we know device address and and device name,
                // we can use the WebSocket to connect all devices in a group

//                synchronized (mLock) {
//                    mSyncService.requestConnectionInfo(info -> {
//                        if (!info.groupFormed) {
//                            mSyncService.connectGroup();
//                        }
//                    });
//                }
                break;
        }
    }

    public List<WifiP2pDevice> getPeers() {
        return mPeers;
    }
}
