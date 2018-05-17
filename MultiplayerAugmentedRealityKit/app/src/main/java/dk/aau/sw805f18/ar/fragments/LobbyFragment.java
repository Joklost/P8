package dk.aau.sw805f18.ar.fragments;

import android.app.FragmentManager;
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

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.common.adapters.LobbyGroupAdapter;
import dk.aau.sw805f18.ar.common.helpers.SyncServiceHelper;
import dk.aau.sw805f18.ar.common.websocket.Packet;
import dk.aau.sw805f18.ar.main.FragmentOpener;
import dk.aau.sw805f18.ar.main.MainActivity;
import dk.aau.sw805f18.ar.services.SyncService;

import static java.lang.Integer.parseInt;


public class LobbyFragment extends Fragment {
    public static final String TAG_LOBBY = "lobby";
    private boolean mAutogrouping = false;

    private static final String[] GROUP_COLOURS = new String[]{
            "#EF5350",
            "#EC407A",
            "#AB47BC",
            "#5C6BC0",
            "#29B6F6",
            "#26C6DA",
            "#26A69A",
            "#9CCC65",
            "#FFEE58",
            "#FFA726",
            "#8D6E63",
            "#78909C",
    };

    private RecyclerView rvGrid;
    private SyncService syncService;
    private Bundle gameOptionsBundle;
    private View lobbyLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lobby, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle bundle = getArguments();
        boolean leader = bundle.containsKey("type") && bundle.get("type") == "leader";

        lobbyLayout = getView().findViewById(R.id.lobby_layout);

        gameOptionsBundle = getArguments();
        syncService = SyncServiceHelper.getInstance();
        syncService.getWebSocket().send(new Packet(Packet.NAME_TYPE, syncService.getDeviceName()));

        rvGrid = getView().findViewById(R.id.lobby_group_recyclerview);
        LobbyGroupAdapter adapter = new LobbyGroupAdapter();

        if (leader) {
            View leaderLayout = getView().findViewById(R.id.leader_layout);
            leaderLayout.setVisibility(View.VISIBLE);

            Button autoGroupButton = getView().findViewById(R.id.lobby_autogrouping_button);
            autoGroupButton.setOnClickListener(v -> {
                String data = mAutogrouping ? "false" : "true";
                syncService.getWebSocket().send(new Packet(Packet.AUTO_GROUP, data));
                autoGroupButton.setText(mAutogrouping ? R.string.lobby_autogroup_start : R.string.lobby_autogroup_stop);
                mAutogrouping = !mAutogrouping;
            });

            Button startButton = getView().findViewById(R.id.lobby_start_button);
            startButton.setVisibility(View.VISIBLE);
            startButton.setOnClickListener(v -> {
                startButton.setText(R.string.lobby_game_starting);
                syncService.getWebSocket().send(new Packet(Packet.START_TYPE,""));

            });
        }

//        adapter.setOnItemClickListener((position, v) -> {
//            try {
//                LobbyDialogFragment dialog = new LobbyDialogFragment();
//                dialog.show(getActivity().getFragmentManager(), "dialog");
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

        syncService.getWebSocket().attachHandler(Packet.AUTO_GROUP, packet -> {
            boolean enabled = packet.Data.equals("true");
            if (enabled) {
                syncService.startAutoGrouping();
            } else {
                syncService.stopAutoGrouping();
            }
        });

        syncService.getWebSocket().attachHandler(Packet.NEWGROUP_TYPE, packet -> {
            int newGroup = parseInt(packet.Data);
            getActivity().runOnUiThread(() -> {
                lobbyLayout.setBackgroundColor(Color.parseColor(GROUP_COLOURS[newGroup]));
            });
            Log.i(TAG_LOBBY, "background color should be set to: " + GROUP_COLOURS[newGroup]);
        });
        syncService.getWebSocket().attachHandler(Packet.OWNER_TYPE, packet -> {
            if (packet.Data.equals("true")) syncService.createWifiP2pGroup();
        });
        syncService.getWebSocket().attachHandler(Packet.MAC_TYPE, packet -> {
            syncService.connectWifiP2p(packet.Data);
        });
        syncService.getWebSocket().attachHandler(Packet.READY_TYPE, packet -> {
            FragmentOpener.getInstance().open(new MapFragment(), MapFragment.TAG);
        });
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
