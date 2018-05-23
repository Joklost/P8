package dk.aau.sw805f18.ar.fragments;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Objects;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.common.adapters.LobbyGroupAdapter;
import dk.aau.sw805f18.ar.common.helpers.SyncServiceHelper;
import dk.aau.sw805f18.ar.common.websocket.Packet;
import dk.aau.sw805f18.ar.common.websocket.WebSocketeer;
import dk.aau.sw805f18.ar.main.FragmentOpener;
import dk.aau.sw805f18.ar.main.MainActivity;
import dk.aau.sw805f18.ar.services.SyncService;

import static java.lang.Integer.parseInt;


public class LobbyFragment extends Fragment {
    public static final String TAG = LobbyFragment.class.getSimpleName();
    public static final String TAG_LOBBY = "lobby";
    private boolean mAutogrouping = false;

    private static final String[] GROUP_COLOURS = new String[]{
            "#EF5350",
            "#29B6F6",
            "#9CCC65",
            "#AB47BC",
            "#26C6DA",
            "#EC407A",
            "#26A69A",
            "#5C6BC0",
            "#FFEE58",
            "#FFA726",
            "#8D6E63",
            "#78909C",
    };

    private SyncService mSyncService;
    private View mLobbyLayout;
    private Activity mActivity;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lobby, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mActivity = getActivity();
        View mView = getView();

        if (mActivity == null || mView == null) {
            Log.e(TAG, "Activity or View was null");
            return;
        }

        Bundle bundle = getArguments();
        boolean leader = Objects.requireNonNull(bundle).containsKey("type") && bundle.get("type") == "leader";

        mLobbyLayout = mView.findViewById(R.id.lobby_layout);

        mSyncService = SyncServiceHelper.getInstance();
        WebSocketeer ws = mSyncService.getWebSocket();

        //region Setup websocket handlers
        ws.attachHandler(Packet.AUTO_GROUP, packet -> {
            boolean enabled = packet.Data.equals("true");
            if (enabled) {
                mSyncService.startAutoGrouping();
            } else {
                mSyncService.stopAutoGrouping();
            }
        });

        ws.attachHandler(Packet.NEWGROUP_TYPE, packet -> {
            int newGroup = parseInt(packet.Data);
            mSyncService.setGroupId(newGroup);
            mActivity.runOnUiThread(() -> {
                ((TextView) mView.findViewById(R.id.groupNumberView)).setText(String.valueOf(newGroup));
                mLobbyLayout.setBackgroundColor(Color.parseColor(GROUP_COLOURS[newGroup]));
            });
        });
        ws.attachHandler(Packet.OWNER_TYPE, packet -> {
            if (packet.Data.equals("true")){
                mSyncService.setOwner(true);
                ws.send(new Packet(Packet.MAC_TYPE, ""));
                ws.send(new Packet(Packet.READY_TYPE, "true"));
            }
        });
        ws.attachHandler(Packet.MAC_TYPE, packet -> {
//            mSyncService.connectWifiP2p(packet.Data, mOwner);
            ws.send(new Packet(Packet.READY_TYPE, "true"));
        });
        ws.attachHandler(Packet.READY_TYPE, packet -> {
            mActivity.runOnUiThread(() -> {
                Toast.makeText(getContext(), R.string.lobby_game_start_toast, Toast.LENGTH_LONG).show();
            });
            FragmentOpener.getInstance().open(new MapFragment(), MapFragment.TAG);
        });
        //endregion

        ws.connect();
        ws.send(new Packet(Packet.NAME_TYPE, mSyncService.getDeviceName()));

        if (leader) {
            View leaderLayout = mView.findViewById(R.id.leader_layout);
            leaderLayout.setVisibility(View.VISIBLE);

            Button autoGroupButton = mView.findViewById(R.id.lobby_autogrouping_button);
            autoGroupButton.setOnClickListener(v -> {
                String data = mAutogrouping ? "false" : "true";
                ws.send(new Packet(Packet.AUTO_GROUP, data));
                mActivity.runOnUiThread(() -> {
                    autoGroupButton.setText(mAutogrouping ? R.string.lobby_autogroup_start : R.string.lobby_autogroup_stop);
                });
                mAutogrouping = !mAutogrouping;
            });

            Button startButton = mView.findViewById(R.id.lobby_start_button);
            startButton.setOnClickListener(v -> {
                try {

                    mActivity.runOnUiThread(() -> {
                        ws.send(new Packet(Packet.START_TYPE, "start"));
                        startButton.setText(R.string.lobby_game_starting);
                        startButton.setEnabled(false);
                    });
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity.CURRENT_FRAGMENT = TAG_LOBBY;
    }

    @Override
    public void onPause() {
        super.onPause();
        mSyncService.getWebSocket().removeHandler(Packet.AUTO_GROUP);
        mSyncService.getWebSocket().removeHandler(Packet.NEWGROUP_TYPE);
        mSyncService.getWebSocket().removeHandler(Packet.MAC_TYPE);
        mSyncService.getWebSocket().removeHandler(Packet.OWNER_TYPE);
        mSyncService.getWebSocket().removeHandler(Packet.READY_TYPE);
    }
}
