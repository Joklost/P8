package dk.aau.sw805f18.ar.main;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.common.adapters.PeerListAdapter;
import dk.aau.sw805f18.ar.services.P2pSyncService;

public class WifiP2pActivity extends AppCompatActivity {
    private static final String TAG = WifiP2pActivity.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private PeerListAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private P2pSyncService mP2pSyncService;
    private boolean mBound;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            P2pSyncService.LocalBinder binder = (P2pSyncService.LocalBinder) service;
            mP2pSyncService = binder.getService();
            mP2pSyncService.init();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mP2pSyncService.deinit();
            mBound = false;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);

        mRecyclerView = findViewById(R.id.peerList);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new PeerListAdapter(null);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Bind P2pSyncService
        if (mBound) {
            return;
        }
        Intent intent = new Intent(this, P2pSyncService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    public void scanPeers(View v) {
        mP2pSyncService.scanPeers(peers -> {
            Log.i(TAG, String.valueOf(peers.getDeviceList().size()));
            mAdapter.setDataset(peers);
            mAdapter.notifyDataSetChanged();
        });
    }
}
