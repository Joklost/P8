package dk.aau.sw805f18.ar.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import dk.aau.sw805f18.ar.common.websocket.Packet;
import dk.aau.sw805f18.ar.common.websocket.WebSocketWrapper;
import dk.aau.sw805f18.ar.common.websocket.WebSocketeer;
import dk.aau.sw805f18.ar.common.wifip2p.WifiP2pReceiver;

public class SyncService extends Service {
    private static final String TAG = SyncService.class.getSimpleName();

    private final IntentFilter mIntentFilter = new IntentFilter();
    private final IBinder mBinder = new LocalBinder();

    private WifiP2pManager.Channel mChannel;
    private WifiP2pManager mManager;
    private String mDeviceAddress;
    private String mDeviceName;


    private String mToken;
    private SocketService mSocketService;
    private HashMap<Integer, ServerSocketService> mServerSocketServices;

    private boolean mIsWifiP2pEnabled;

    private WebSocketWrapper mWebSocket;
    private WebSocketeer mWebSocketeer;

    public WifiP2pReceiver getReceiver() {
        return mReceiver;
    }

    private WifiP2pReceiver mReceiver;

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

//        mWebSocket = WebSocketWrapper.getInstance("http://warpapp.xyz/connect/test");
    }

    public void attachHandler(String type, Consumer<Packet> handler) {
        mWebSocketeer.attachHandler(type, handler);
    }
    public void send(Packet packet) {
        mWebSocketeer.send(packet);
    }

    public void connectGroup() {
//        mWebSocket.sendPacket(new Packet(Packet.NAME_TYPE, mDeviceName));

        Log.i(TAG, "Waiting for initial packet...");
        Packet packet = mWebSocket.waitPacket();

        // TODO: packet should contain color code, make enumerable
//        switch (packet.Type) {
//            case Packet.OWNER_TYPE:
//                if (packet.Data.equals(Packet.TRUE)) {
//                    Log.i(TAG, "Creating group...");
//                    createGroup();
//                    Log.i(TAG, "Group created, transmitting Device Address: " + mDeviceAddress);
//                    mWebSocket.sendPacket(new Packet(Packet.MAC_TYPE, mDeviceAddress));
//                    Log.i(TAG, "Awaiting connections");
//
//                    completeGroup();
//                }
//                break;
//            case Packet.MAC_TYPE:
//                if (packet.Data != null) {
//                    try {
//                        new Handler().postDelayed(() -> {
//                            Log.i(TAG, "Connecting to: " + packet.Data);
//                            connect(packet.Data);
//                        }, 3000);
//                    } catch (Exception e) {
//                        Log.e(TAG, e.toString());
//                    }
//
//                } else {
//                    Log.e(TAG, "Received 'mac' packet without Device Address!");
//                }
//                break;
//        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        mIsWifiP2pEnabled = isWifiP2pEnabled;
    }

    public void connect(WifiP2pDevice device) {
        connect(device.deviceAddress);
    }

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

    public void setDeviceAddress(String deviceAddress) {
        this.mDeviceAddress = deviceAddress;
    }

    public void setDeviceName(String deviceName) {
        mDeviceName = deviceName;
    }

    public void joinGroup(String token) {
        String masterAddress;
        int port;

        mWebSocket.sendPacket(new Packet(Packet.JOIN_TYPE, token + ";" + mDeviceAddress));
        Log.i(TAG, "Sent Join Packet, waiting for response.");

        Packet packet = mWebSocket.waitPacket();
        if (packet.Type.equals(Packet.OK_TYPE)) {
            // TODO: set spinner or something while waiting
            String[] data = packet.Data.split(",");
            masterAddress = data[0];
            port = Integer.parseInt(data[1]);

            packet = mWebSocket.waitPacket();

            if (packet.Type.equals(Packet.GROUP_COMPLETED_TYPE)) {
                connect(masterAddress);
            }
        }
    }

    public class LocalBinder extends Binder {
        public SyncService getService() {
            return SyncService.this;
        }
    }

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

    @Override
    public void onDestroy() {
        deinit();
        super.onDestroy();
    }

    private boolean mDiscoverInitiated;
    private boolean mGroupCreated;

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

    public void requestPeers(WifiP2pManager.PeerListListener listener) {
        mManager.requestPeers(mChannel, listener);
    }

    public void requestConnectionInfo(WifiP2pManager.ConnectionInfoListener listener) {
        mManager.requestConnectionInfo(mChannel, listener);
    }

    public void requestGroupInfo(WifiP2pManager.GroupInfoListener listener) {
        mManager.requestGroupInfo(mChannel, listener);
    }


    public void createGroup() {
        if (mGroupCreated) return;

        // Send start group packet to server, with own device address
        mWebSocket.sendPacket(new Packet(Packet.CREATE_TYPE, mDeviceAddress));

        Packet packet = mWebSocket.waitPacket();
        if (packet.Type.equals(Packet.OWNER_TYPE)) mToken = packet.Data;
        else if (packet.Type.equals(Packet.ERROR_TYPE)) Log.e(TAG, packet.Data);
        else Log.e(TAG, "Invalid packet recieved");

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
    public HashMap<String, Integer> completeGroup() {
        mWebSocket.sendPacket(new Packet(Packet.GROUP_COMPLETED_TYPE, ""));
        int port = 5000;
        HashMap<String, Integer> portMap = new HashMap<String, Integer>();

        List<WifiP2pDevice> peers = getReceiver().getPeers();
        ServerSocketService socket;

        for (WifiP2pDevice peer : peers) {
            if (peer.deviceAddress.equals(mDeviceAddress)) continue;
            portMap.put(peer.deviceAddress, port);
            socket = new ServerSocketService(peer.deviceAddress, port++);
            socket.run();
        }

        return portMap;
    }

    public String getToken() {
        return mToken;
    }
}

