package dk.aau.sw805f18.ar.fragments;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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
import dk.aau.sw805f18.ar.main.MainActivity;
import dk.aau.sw805f18.ar.common.helpers.RunnableExecutor;
import dk.aau.sw805f18.ar.common.websocket.WebSocketeer;
import dk.aau.sw805f18.ar.main.DialogLobby;



public class LobbyFragment extends Fragment {
    public static final String TAG_LOBBY = "lobby";


    private static final int[] GROUP_COLROS = new int[] {
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lobby, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle gameOptionsBundle = getArguments();

        RecyclerView rvGrid = getView().findViewById(R.id.lobby_group_recyclerview);
        LobbyGroupAdapter adapter = new LobbyGroupAdapter();
        String[] data = {"mBoi", "bitte", "øøøh"};

        switch (gameOptionsBundle.getString(CreateCourseFragment.GROUPING)) {
            case "Troop leader":
                break;
            case "Self selection":
                break;
            case "Auto grouping":
                // Start sending GPS coordinates to server


                // Should be the attached websocket instead of this "mock" instance
                //new WebSocketeer("").attachHandler("newgroup", packet -> {
                //    int newGroup = parseInt(packet.Data);
                //    rvGrid.setBackgroundColor(GROUP_COLROS[newGroup]);
                //});
                break;
        }

        adapter.setOnItemClickListener((position, v) -> {
            LobbyDialogFragment dialog = new LobbyDialogFragment();
            adapter.setOnItemClickListener((position, v) -> {
                DialogLobby dialog = new DialogLobby();
                android.app.FragmentManager fragmentManager = getActivity().getFragmentManager();
                dialog.show(fragmentManager, "dialog");
            });

            rvGrid.setAdapter(adapter);
            rvGrid.setLayoutManager(new GridLayoutManager(getContext(), 2));


            // Thread for sending position data, when auto grouping
            RunnableExecutor.getInstance().execute(() -> {

            });

            // Thread for receiving group data, when auto grouping
            RunnableExecutor.getInstance().execute(() -> {

            });

        });


    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity.CURRENT_FRAGMENT = TAG_LOBBY;
    }
}
