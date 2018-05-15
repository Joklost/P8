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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import dk.aau.sw805f18.ar.ar.location.sensor.DeviceLocation;
import dk.aau.sw805f18.ar.common.websocket.Packet;
import dk.aau.sw805f18.ar.common.websocket.WebSocketeer;
import dk.aau.sw805f18.ar.common.wifip2p.WifiP2pReceiver;
import dk.aau.sw805f18.ar.models.AutoGrouping;

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

    public WebSocketeer mWebSocketeer;
    private AutoGrouping mAutoGrouping;
    private List<WifiP2pDevice> mPeers = new ArrayList<>();

    private WifiP2pReceiver mReceiver;
    private DeviceLocation mDeviceLocation;

    public SyncService() {
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION); // Whether Wifi P2P is enabled or not
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION); // Fires when peer list has changed
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION); // Indicates that state of Wifi P2P connection has changed
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION); // This device's details have changed

        try {
            mWebSocketeer = new WebSocketeer("http://warpapp.xyz/connect/test");
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Failed to instantiate WebSocketeer.");
            Log.e(TAG, e.getMessage());
        }
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

        mAutoGrouping = new AutoGrouping(mDeviceLocation, mWebSocketeer);

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
     * Helper function for WebSocketeer access.
     *
     * @param type    The type of Packet the handler should handle.
     * @param handler Handles the Packet.
     */
    public void attachHandler(String type, Consumer<Packet> handler) {
        mWebSocketeer.attachHandler(type, handler);
    }

    /**
     * Helper function for WebSocketeer access.
     *
     * @param packet The Packet to be sent.
     */
    public void send(Packet packet) {
        mWebSocketeer.send(packet);
    }


    /**
     * Binder implementation for SyncService.
     */
    public class LocalBinder extends Binder {
        public SyncService getService() {
            return SyncService.this;
        }
    }

//    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
//        mIsWifiP2pEnabled = isWifiP2pEnabled;
//    }

    /**
     * Connects to a device using WifiP2P.
     *
     * @param device The device to connect to.
     */
    public void connect(WifiP2pDevice device) {
        connect(device.deviceAddress);
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

    public void discoverPeers() {
        if (mDiscoverInitiated) {
            return;
        }

        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mDiscoverInitiated = true;
                Log.i(TAG, "Successfully initiated peer discovery.");
            }

            @Override
            public void onFailure(int reason) {
                mDiscoverInitiated = false;
                Log.e(TAG, "Failed to initiate peer discovery.");
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

    public void txSocket(Packet packet) {
        Log.i(TAG, "txSocket()");
        mWebSocketeer.send(packet);
    }

    /**
     * Setup sockets to each peer, for master device
     * @return Device address to port mapping
     */
    public HashMap<String, Integer> serverSocketSetup() {
        mWebSocketeer.send(new Packet(Packet.GROUP_COMPLETED_TYPE, ""));
        int port = PORT;
        HashMap<String, Integer> portMap = new HashMap<>();

        ServerSocketService socket;

        for (WifiP2pDevice peer : mPeers) {
            if (peer.deviceAddress.equals(mDeviceAddress)) continue;

            portMap.put(peer.deviceAddress, port);
            socket = new ServerSocketService(peer.deviceAddress, port++);
            socket.run();
        }

        return portMap;
    }

    /**
     * Setup socket for client
     */
    public void socketSetup(InetSocketAddress address) {
        Context context = getApplicationContext();

        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                SocketService.LocalBinder binder = (SocketService.LocalBinder) service;
                mSocketService = binder.getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.e(TAG, "Socket service disconnected");
            }
        };
        Intent intent = new Intent(context, SocketService.class);
        intent.putExtra("address", address.getAddress().toString());
        intent.putExtra("port", address.getPort());

//        intent.putExtra("address", address.toString());
        context.bindService(intent, connection, context.BIND_AUTO_CREATE);
    }

    public void wifitest() {
//        Name: displayname, Team: #number
        send(new Packet(Packet.SET_GROUP_TYPE, String.format("{Name: %s, Team: 0}", mDeviceName)));
        send(new Packet(Packet.START_TYPE, ""));

        attachHandler(Packet.OWNER_TYPE, packet -> {
            Log.i(TAG, "Owner packet received. Data: " + packet.Data);

            if (packet.Data.equals("true")) {
                Log.i(TAG, "Creating group");
                createGroup();
                Log.i(TAG, "Sent MAC packet");
                send(new Packet(Packet.MAC_TYPE, mDeviceAddress));
                Log.i(TAG, "Sent READY packet");
                send(new Packet(Packet.READY_TYPE, ""));
            } else {
                attachHandler(Packet.MAC_TYPE, packet1 -> {
                    Log.i(TAG, "Connecting to group");
                    connect(packet1.Data);
                    Log.i(TAG, "Sent READY packet");
                    send(new Packet(Packet.READY_TYPE, ""));
                });
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

