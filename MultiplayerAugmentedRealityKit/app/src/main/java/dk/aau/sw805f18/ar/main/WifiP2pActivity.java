package dk.aau.sw805f18.ar.main;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.common.adapters.PeerListAdapter;
import dk.aau.sw805f18.ar.services.SyncService;

public class WifiP2pActivity extends AppCompatActivity {
    private static final String TAG = WifiP2pActivity.class.getSimpleName();

    private PeerListAdapter mAdapter;

    private SyncService mSyncService;
    private boolean mBound;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SyncService.LocalBinder binder = (SyncService.LocalBinder) service;
            mSyncService = binder.getService();
            mSyncService.init();

            if (mAdapter != null) {
                mSyncService.getReceiver().setAdapter(mAdapter);
            }

            mSyncService.discoverPeers();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mSyncService.deinit();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);

        RecyclerView recyclerView = findViewById(R.id.peerList);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)
        mAdapter = new PeerListAdapter();
        recyclerView.setAdapter(mAdapter);

        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(this, (view, position) -> {
            Log.i(TAG, String.valueOf(((TextView) view).getText()));
            WifiP2pDevice device = mAdapter.getDataset().get(position);
            mSyncService.connect(device);
        }));
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Bind SyncService
        if (mBound) {
            return;
        }
        Intent intent = new Intent(this, SyncService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mBound = true;


    }

    @Override
    protected void onPause() {
        super.onPause();

        if (!mBound) {
            return;
        }

        unbindService(mConnection);
        mBound = false;
    }

    public void scanPeers(View v) {
        mSyncService.discoverPeers();
    }

    public void requestGroupInfo(View v) {
        mSyncService.requestGroupInfo(group -> {
            if (group == null) {
                Log.i(TAG, "No groups created");
                return;
            }
            Log.i(TAG, group.getOwner().deviceName);
        });
    }

    public void createGroup(View v) {
        mSyncService.createGroup();
    }
}
