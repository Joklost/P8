package dk.aau.sw805f18.ar.fragments;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.services.SyncService;


public class GroupFragment extends Fragment {
    public static final String TAG_GROUP = "group";
    private TextView mMake, mJoin;
    private Button mDone;
    private String mToken;
    private SyncService mSyncService;
    private boolean mBound;
    private HashMap<String, Integer> mPortMap;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SyncService.LocalBinder binder = (SyncService.LocalBinder) service;
            mSyncService = binder.getService();
            mSyncService.init();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mSyncService.deinit();
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity a = getActivity();
        // Bind SyncService
        if (mBound) {
            return;
        }
        Intent intent = new Intent(a, SyncService.class);
        a.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mBound = true;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_group, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // For now, Create Group doesn't trigger a new view, but instead the code that should happen in that view


        mMake = getView().findViewById(R.id.group_make_new);
        mJoin = getView().findViewById(R.id.group_join);
        mDone = getView().findViewById(R.id.group_done);

        mMake.setOnClickListener(v -> {
            mSyncService.createWifiP2pGroup();
            if ((mToken = mSyncService.getToken()) == null) {
                Toast.makeText(getContext(), "Something went wrong, please try again", Toast.LENGTH_LONG).show();
            }
        });

        // TODO: Fix hardcoded token for join (debug)
        mJoin.setOnClickListener(v -> {
//            mSyncService.joinGroup("NIGGA", 1);

        });

    }
}
