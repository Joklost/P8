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

    private SyncService mSyncService;
    private boolean mBound;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SyncService.LocalBinder binder = (SyncService.LocalBinder) service;
            mSyncService = binder.getService();
            mSyncService.init();

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

    public void joinGroup(View v) {
//        mSyncService.joinGroup("NIGGA", 1);
    }

    public void createGroup(View v) {
        mSyncService.createGroup();
    }

    public void testWifiP2p(View v) {

    }
}
