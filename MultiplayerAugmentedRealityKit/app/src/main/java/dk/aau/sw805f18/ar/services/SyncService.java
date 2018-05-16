package dk.aau.sw805f18.ar.services;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import dk.aau.sw805f18.ar.ar.location.sensor.DeviceLocation;
import dk.aau.sw805f18.ar.common.websocket.Packet;
import dk.aau.sw805f18.ar.common.websocket.WebSocketeer;
import dk.aau.sw805f18.ar.common.websocket.WebSocketeerServer;
import dk.aau.sw805f18.ar.common.wifip2p.WifiP2pReceiver;
import dk.aau.sw805f18.ar.models.AutoGrouping;
import dk.aau.sw805f18.ar.models.PacketSetupGroup;

public class SyncService extends Service {
    private static final String TAG = SyncService.class.getSimpleName();

    private final IntentFilter mIntentFilter = new IntentFilter();
    private final IBinder mBinder = new LocalBinder();

    private WifiP2pManager.Channel mChannel;
    private WifiP2pManager mManager;

    private String mDeviceAddress;
    private String mDeviceName;
    private boolean mDiscoverInitiated;
    private boolean mGroupCreated;
    private String mToken;
    private SocketService mSocketService;
    private HashMap<Integer, ServerSocketService> mServerSocketServices;

    private final int PORT = 5000;

//    private boolean mIsWifiP2pEnabled;

    private Gson mJson = new Gson();

    public WebSocketeer getWebSocket() {
        return mWebSocket;
    }

    public WebSocketeer mWebSocket;
    public WebSocketeer mWifiP2PSocket;
    public WebSocketeerServer mWebSocketeerServer;
    private AutoGrouping mAutoGrouping;
    private List<WifiP2pDevice> mPeers = new ArrayList<>();

    private WifiP2pReceiver mReceiver;
    private DeviceLocation mDeviceLocation;

    public SyncService() {
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION); // Whether Wifi P2P is enabled or not
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION); // Fires when peer list has changed
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION); // Indicates that state of Wifi P2P connection has changed
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION); // This device's details have changed
    }

    public void joinLobby(String lobbyId) throws ExecutionException, InterruptedException {
        if (mWebSocket != null) {
            mWebSocket.close();
        }
        mWebSocket = new WebSocketeer("http://warpapp.xyz/connect/" + lobbyId);
    }

    /**
     * Initialises the service by starting WifiP2pReceiver, WifiP2pManager and AutoGrouping.
     */
    public void init() {
        if (mReceiver != null) {
            return;
        }
        mReceiver = new WifiP2pReceiver(this);
        registerReceiver(mReceiver, mIntentFilter);

        if (mManager != null) {
            return;
        }
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);

        if (mChannel != null) {
            return;
        }
        mChannel = mManager.initialize(this, getMainLooper(), null);

        requestConnectionInfo(info -> {
            if (info != null) {
                Log.i(TAG, "Connection already exists?");
                Log.i(TAG, info.toString());
            }
        });

        mAutoGrouping = new AutoGrouping(mDeviceLocation, mWebSocket);

        // Remove any existing group, so a new one can be created.
        removeGroup();
    }

    public void deinit() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }

        if (Build.VERSION.SDK_INT > 26) {
            // WifiP2pManager.Channel.close() was added in API level 27.
            if (mChannel != null) {
                mChannel.close();
                mChannel = null;
            }
        }
    }


    /**
     * Binder implementation for SyncService.
     */
    public class LocalBinder extends Binder {
        public SyncService getService() {
            return SyncService.this;
        }
    }

    /**
     * Connects to a device using WifiP2P.
     *
     * @param deviceAddress The address of the device to connect to.
     */
    public void connect(String deviceAddress) {
        if (deviceAddress == null) {
            Log.e(TAG, "DEVICE IS NULL");
            return;
        }

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Successfully connected to: " + config.deviceAddress);
            }

            @Override
            public void onFailure(int reason) {
                Log.i(TAG, "Failed to connect to: " + config.deviceAddress);
            }
        });
    }


    public void requestPeers() {
        mManager.requestPeers(mChannel, peerList -> {

            // Out with the old, in with the new.
            mPeers.clear();
            mPeers.addAll(peerList.getDeviceList());

            // If an AdapterView is backed by this Data, notify it
            // of the change.  For instance, if you have a ListView of available
            // peers, trigger an update.

            if (mPeers.size() == 0) {
                Log.d(TAG, "No devices found");
            }
        });
    }

    public void requestConnectionInfo(WifiP2pManager.ConnectionInfoListener listener) {
        mManager.requestConnectionInfo(mChannel, listener);
    }

    public void requestGroupInfo(WifiP2pManager.GroupInfoListener listener) {
        mManager.requestGroupInfo(mChannel, listener);
    }

    /**
     * Creates a Wifi P2P group, if one doesn't exist already.
     */
    public void createGroup() {
        if (mGroupCreated) return;

        mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Successfully created group.");
                mGroupCreated = true;
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Failed to create group.");
                mGroupCreated = false;
            }
        });
    }

    /**
     * Removes an existing Wifi P2P group.
     */
    public void removeGroup() {
        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Successfully removed group.");
                mGroupCreated = false;
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Failed to remove group.");
            }
        });
    }

    public void startAutoGrouping() {
        mAutoGrouping.start();
    }

    public void stopAutoGrouping() {
        mAutoGrouping.stop();
    }

    //region overrides
    @Override
    public void onDestroy() {
        deinit();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    //endregion

    //region getters and setters
    public void setDeviceAddress(String deviceAddress) {
        this.mDeviceAddress = deviceAddress;
    }

    public void setDeviceName(String deviceName) {
        mDeviceName = deviceName;
    }

    public String getToken() {
        return mToken;
    }

    public void setDeviceLocation(DeviceLocation dl) {
        mDeviceLocation = dl;
    }
    //endregion
}

