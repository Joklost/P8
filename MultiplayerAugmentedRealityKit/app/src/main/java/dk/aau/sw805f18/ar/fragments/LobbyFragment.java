package dk.aau.sw805f18.ar.fragments;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
            "#AB47BC",
            "#29B6F6",
            "#26C6DA",
            "#EC407A",
            "#26A69A",
            "#9CCC65",
            "#5C6BC0",
            "#FFEE58",
            "#FFA726",
            "#8D6E63",
            "#78909C",
    };

    private RecyclerView rvGrid;
    private SyncService syncService;
    private Bundle gameOptionsBundle;
    private View lobbyLayout;
    private boolean mOwner = false;
    private Activity mActivity;
    private View mView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lobby, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mActivity = getActivity();
        mView = getView();

        if (mActivity == null || mView == null) {
            Log.e(TAG, "Activity or View was null");
            return;
        }

        Bundle bundle = getArguments();
        boolean leader = Objects.requireNonNull(bundle).containsKey("type") && bundle.get("type") == "leader";

        lobbyLayout = mView.findViewById(R.id.lobby_layout);

        gameOptionsBundle = getArguments();
        syncService = SyncServiceHelper.getInstance();
        WebSocketeer ws = syncService.getWebSocket();

        //region Setup websocket handlers
        ws.attachHandler(Packet.AUTO_GROUP, packet -> {
            boolean enabled = packet.Data.equals("true");
            if (enabled) {
                syncService.startAutoGrouping();
            } else {
                syncService.stopAutoGrouping();
            }
        });

        ws.attachHandler(Packet.NEWGROUP_TYPE, packet -> {
            int newGroup = parseInt(packet.Data);
            syncService.setGroupId(newGroup);
            mActivity.runOnUiThread(() -> {
                lobbyLayout.setBackgroundColor(Color.parseColor(GROUP_COLOURS[newGroup]));
            });
        });
        ws.attachHandler(Packet.OWNER_TYPE, packet -> {
            if (packet.Data.equals("true")){
                syncService.setOwner(true);
                ws.send(new Packet(Packet.MAC_TYPE, ""));
                ws.send(new Packet(Packet.READY_TYPE, "true"));
            }
        });
        ws.attachHandler(Packet.MAC_TYPE, packet -> {
//            syncService.connectWifiP2p(packet.Data, mOwner);
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
        ws.send(new Packet(Packet.NAME_TYPE, syncService.getDeviceName()));

        rvGrid = mView.findViewById(R.id.lobby_group_recyclerview);
        LobbyGroupAdapter adapter = new LobbyGroupAdapter();


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

//        adapter.setOnItemClickListener((position, v) -> {
//            try {
//                LobbyDialogFragment dialog = new LobbyDialogFragment();
//                dialog.show(mActivity.getFragmentManager(), "dialog");
//            } catch (Exception e) {
//                Log.e("DialogFragment", e.toString());
//            }

//        });

        rvGrid.setAdapter(adapter);
        rvGrid.setLayoutManager(new GridLayoutManager(getContext(), 2));
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity.CURRENT_FRAGMENT = TAG_LOBBY;
    }

    @Override
    public void onPause() {
        super.onPause();
        syncService.getWebSocket().removeHandler(Packet.AUTO_GROUP);
        syncService.getWebSocket().removeHandler(Packet.NEWGROUP_TYPE);
        syncService.getWebSocket().removeHandler(Packet.MAC_TYPE);
        syncService.getWebSocket().removeHandler(Packet.OWNER_TYPE);
        syncService.getWebSocket().removeHandler(Packet.READY_TYPE);
    }
}
