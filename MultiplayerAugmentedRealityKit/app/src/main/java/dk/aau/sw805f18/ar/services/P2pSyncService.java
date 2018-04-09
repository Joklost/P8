package dk.aau.sw805f18.ar.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Collection;

import static com.google.vr.dynamite.client.Version.TAG;

public class P2pSyncService extends Service {
    private static final String TAG = P2pSyncService.class.getSimpleName();

    private final IntentFilter mIntentFilter = new IntentFilter();
    private final IBinder mBinder = new LocalBinder();

    private WifiP2pManager.Channel mChannel;
    private WifiP2pManager mManager;
    private boolean mIsWifiP2pEnabled;
    private WifiP2pReceiver mReceiver;

    public P2pSyncService() {
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        // Indicates a change in the list of available peers.
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // Indicates this device's details have changed.
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        mIsWifiP2pEnabled = isWifiP2pEnabled;
    }


    public class LocalBinder extends Binder {
        public P2pSyncService getService() {
            return P2pSyncService.this;
        }
    }

    public void init() {
        if (mManager != null) {
            return;
        }
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);

        if (mChannel != null) {
            return;
        }
        mChannel = mManager.initialize(this, getMainLooper(), null);

        if (mReceiver != null) {
            return;
        }
        mReceiver = new WifiP2pReceiver(mManager, mChannel, this);
        registerReceiver(mReceiver, mIntentFilter);
    }

    public void deinit() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        if (mChannel != null) {
            mChannel.close();
            mChannel = null;
        }
    }

    @Override
    public void onDestroy() {
        deinit();
        super.onDestroy();
    }

    public WifiP2pManager.Channel getChannel() {
        return mChannel;
    }

    public void scanPeers(WifiP2pManager.PeerListListener listener) {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mManager.requestPeers(mChannel, listener);
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Failed to discover peers!");
            }
        });
    }
}

