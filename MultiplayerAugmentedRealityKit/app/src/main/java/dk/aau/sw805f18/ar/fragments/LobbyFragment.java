package dk.aau.sw805f18.ar.fragments;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.common.adapters.LobbyGroupAdapter;
import dk.aau.sw805f18.ar.common.helpers.SyncServiceHelper;
import dk.aau.sw805f18.ar.main.MainActivity;
import dk.aau.sw805f18.ar.services.SyncService;

import static java.lang.Integer.parseInt;


public class LobbyFragment extends Fragment {
    public static final String TAG_LOBBY = "lobby";

    private static final int[] GROUP_COLOURS = new int[]{
            0xEF5350,
            0xEC407A,
            0xAB47BC,
            0x5C6BC0,
            0x29B6F6,
            0x26C6DA,
            0x26A69A,
            0x9CCC65,
            0xFFEE58,
            0xFFA726,
            0x8D6E63,
            0x78909C,
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

        gameOptionsBundle = getArguments();
        syncService = SyncServiceHelper.getInstance();

        rvGrid = getView().findViewById(R.id.lobby_group_recyclerview);
        LobbyGroupAdapter adapter = new LobbyGroupAdapter();
        String[] data = {"mBoi", "bitte", "øøøh"};

        lobbyLayout = getView().findViewById(R.id.lobby_layout);
        switch (gameOptionsBundle.getString(CreateCourseFragment.GROUPING)) {
            case "Troop leader":
                break;
            case "Self selection":
                break;
            case "Auto grouping":
                // Start sending GPS coordinates to server

                syncService.mWebSocketeer.attachHandler("newgroup", packet -> {
                    int newGroup = parseInt(packet.Data);
                    lobbyLayout.setBackgroundColor(GROUP_COLOURS[newGroup]);
                });
                break;
        }

        adapter.setOnItemClickListener((position, v) -> {
            LobbyDialogFragment dialog = new LobbyDialogFragment();

            FragmentManager fragmentManager = getActivity().getFragmentManager();
            dialog.show(fragmentManager, "dialog");
        });

        rvGrid.setAdapter(adapter);
        rvGrid.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // set up handlers for

    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity.CURRENT_FRAGMENT = TAG_LOBBY;

        syncService.mWebSocketeer.attachHandler("autogroup", packet -> {
            boolean enabled = packet.Data.equals("true");
            // start or stop autogroup gps loop on syncService
        });

        syncService.mWebSocketeer.attachHandler("newgroup", packet -> {
            int newGroup = parseInt(packet.Data);
            lobbyLayout.setBackgroundColor(GROUP_COLOURS[newGroup]);
        });
    }

    @Override
    public void onPause() {
        super.onResume();
        syncService.mWebSocketeer.removeHandler("autogroup");
        syncService.mWebSocketeer.removeHandler("newgroup");
    }
}
