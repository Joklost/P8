package dk.aau.sw805f18.ar.services;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import dk.aau.sw805f18.ar.common.sensor.DeviceLocation;
import dk.aau.sw805f18.ar.common.websocket.Packet;
import dk.aau.sw805f18.ar.common.websocket.WebSocketeer;
import dk.aau.sw805f18.ar.common.websocket.WebSocketeerServer;
import dk.aau.sw805f18.ar.common.wifip2p.WifiP2pReceiver;
import dk.aau.sw805f18.ar.models.AutoGrouping;
import dk.aau.sw805f18.ar.models.Player;

public class SyncService extends Service {
    private static final String TAG = SyncService.class.getSimpleName();

    private final IntentFilter mIntentFilter = new IntentFilter();
    private final IBinder mBinder = new LocalBinder();

    private WifiP2pManager.Channel mChannel;
    private WifiP2pManager mManager;
    private String mDeviceAddress;
    private String mDeviceName;
    private int mGroupId;
    private boolean mDiscoverInitiated;
    private boolean mGroupCreated;
    private Gson mJson = new Gson();
    private WebSocketeer mWebSocket;
    private WebSocketeer mWifiP2pSocket;
    private WebSocketeerServer mWebSocketeerServer;
    private AutoGrouping mAutoGrouping;
    private WifiP2pReceiver mReceiver;
    private DeviceLocation mDeviceLocation;
    private List<Player> mPlayers;
    private boolean mOwner;

    public SyncService() {
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION); // Whether Wifi P2P is enabled or not
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION); // Fires when peer list has changed
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION); // Indicates that state of Wifi P2P connection has changed
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION); // This device's details have changed
    }

    //region getters and setters
    public void setDeviceAddress(String deviceAddress) {
        this.mDeviceAddress = deviceAddress;
    }

    public boolean IsLeader() {
        return mOwner;
    }

    public void setOwner(boolean owner) {
        mOwner = owner;
    }

    public void joinLobby(String lobbyId) throws ExecutionException, InterruptedException {
        if (mWebSocket != null) {
            mWebSocket.close();
        }
        mWebSocket = new WebSocketeer("http://warpapp.xyz/connect/" + lobbyId);

        mWebSocket.attachHandler(Packet.PLAYERS_TYPE, packet -> {
            mPlayers = mJson.fromJson(packet.Data, new TypeToken<List<Player>>() {
            }.getType());
            Log.i(TAG, "mplayers " + mPlayers.size());
        });
    }

    /**
     * Initialises the service by starting WifiP2pReceiver, WifiP2pManager and AutoGrouping.
     *
     * @param activity The activity used to bind this service.
     */
    public void init(Activity activity) {
        if (mReceiver != null) {
            return;
        }
        mReceiver = new WifiP2pReceiver(this);
        registerReceiver(mReceiver, mIntentFilter);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(activity, getMainLooper(), null);
        mAutoGrouping = new AutoGrouping(mDeviceLocation);

        // Remove any existing group, so a new one can be created.
        removeWifiP2pGroup();
        discoverPeers();
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
     * Connects to a device using WifiP2P.
     *
     * @param deviceAddress The address of the device to connectWifiP2p to.
     */
    public void connectWifiP2p(String deviceAddress, boolean ownerIntent) {
        if (deviceAddress == null) {
            Log.e(TAG, "DEVICE IS NULL");
            return;
        }

        removeWifiP2pGroup();
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        config.groupOwnerIntent = ownerIntent ? 15 : 0;

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Successfully connected to: " + config.deviceAddress);

                if (mWifiP2pSocket != null) {
                    mWifiP2pSocket.close();
                    mWifiP2pSocket = null;
                }

                mManager.requestConnectionInfo(mChannel, info -> {
                    Log.i(TAG, " isGroupFormed: " + info.groupFormed);
                    Log.i(TAG, info.groupOwnerAddress.getHostAddress());
                    String ip = "http://" + info.groupOwnerAddress.getHostAddress() + ":5676/connect";
                    Log.i(TAG, "Attempting to connect to " + ip);
                    try {
                        mWifiP2pSocket = new WebSocketeer(ip);
                        mWifiP2pSocket.connect();
                        mWebSocket.send(new Packet(Packet.READY_TYPE, "true"));
                    } catch (ExecutionException | InterruptedException e) {
                        Log.e(TAG, e.getMessage());
                    }
                });


            }

            @Override
            public void onFailure(int reason) {
                Log.i(TAG, "Failed to connectWifiP2p to: " + config.deviceAddress + ", REASON=" + reason);
            }
        });
    }

    public void requestConnectionInfo(WifiP2pManager.ConnectionInfoListener listener) {
        mManager.requestConnectionInfo(mChannel, listener);
    }

    public void requestGroupInfo(WifiP2pManager.GroupInfoListener listener) {
        mManager.requestGroupInfo(mChannel, listener);
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

    /**
     * Creates a Wifi P2P group, if one doesn't exist already.
     */
    public void createWifiP2pGroup() {
        if (mGroupCreated) return;


        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mGroupCreated = false;
                createWifiP2pGroupHelper();
            }

            @Override
            public void onFailure(int reason) {
                createWifiP2pGroupHelper();
            }
        });


    }

    private void createWifiP2pGroupHelper() {
        mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Successfully created group.");
                mGroupCreated = true;

                if (mWifiP2pSocket != null) {
                    mWifiP2pSocket.close();
                    mWifiP2pSocket = null;
                }
                mWebSocketeerServer = new WebSocketeerServer();
                mWebSocketeerServer.start();
                // TODO: Her skal der være nogle handlers for gruppe ws connectivity


                mWebSocket.send(new Packet(Packet.MAC_TYPE, mDeviceAddress));
                mWebSocket.send(new Packet(Packet.READY_TYPE, "true"));
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
    public void removeWifiP2pGroup() {
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

    public String getDeviceName() {
        return mDeviceName;
    }

    public void setDeviceName(String deviceName) {
        mDeviceName = deviceName;
    }

    public void setDeviceLocation(DeviceLocation dl) {
        mDeviceLocation = dl;
    }

    public WebSocketeer getWebSocket() {
        return mWebSocket;
    }

    public WebSocketeer getWifiP2pSocket() {
        return mWifiP2pSocket;
    }

    public WebSocketeerServer getWebSocketeerServer() {
        return mWebSocketeerServer;
    }

    public boolean isHostingWifiP2p() {
        return mWebSocketeerServer != null;
    }

    public void setGroupId(int mGroupId) {
        this.mGroupId = mGroupId;
    }

    public List<Player> getPlayersOnTeam() {
        List<Player> players = new ArrayList<>();
        for (Player player : mPlayers) {
            if (player.Team == mGroupId) {
                players.add(player);
            }
        }
        Log.i(TAG, "Der er " + players.size() + " spillere på dit hold");
        return players;
    }

    /**
     * Binder implementation for SyncService.
     */
    public class LocalBinder extends Binder {
        public SyncService getService() {
            return SyncService.this;
        }
    }

    //endregion
}

