package dk.aau.sw805f18.ar.fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.common.helpers.SyncServiceHelper;
import dk.aau.sw805f18.ar.main.FragmentOpener;
import dk.aau.sw805f18.ar.main.MainActivity;
import dk.aau.sw805f18.ar.services.SyncService;

public class CreateCourseFragment extends Fragment {
    public static final String TAG_CREATE = "createcourse";
    public static final String TAG_ROLE = "leader";

    public static final String GROUPING = "grouping";
    public static final String GROUPS = "groups";
    public static final String MAX_PLAYERS = "maxPlayers";
    public static final String LOBBY_ID = "lobbyId";
    private SyncService syncService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_course, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Spinner groupSpinner = getView().findViewById(R.id.create_course_group_selection_spinner);

        ArrayAdapter<CharSequence> groupSelectAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.create_course_group_selection_array,
                android.R.layout.simple_spinner_item);

        groupSelectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupSpinner.setAdapter(groupSelectAdapter);

        Button createBtn = getView().findViewById(R.id.create_course_create_button);
        createBtn.setOnClickListener(v -> {
            String selectedGroupingMethod = (String) groupSpinner.getSelectedItem();
            int numberOfTeams = 3;
            int maxPlayers = 16;
            String lobbyId = "test";

            Bundle gameOptionBundle = new Bundle();
            gameOptionBundle.putString(GROUPING, selectedGroupingMethod);
            gameOptionBundle.putString("type", TAG_ROLE);
            gameOptionBundle.putInt(GROUPS, numberOfTeams);
            gameOptionBundle.putInt(MAX_PLAYERS, maxPlayers);
            gameOptionBundle.putString(LOBBY_ID, lobbyId);
            syncService = SyncServiceHelper.getInstance();

            LobbyFragment lobbyFragment = new LobbyFragment();
            lobbyFragment.setArguments(gameOptionBundle);
            try {
                syncService.joinLobby(lobbyId);
                FragmentOpener.getInstance().open(lobbyFragment, TAG_CREATE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity.CURRENT_FRAGMENT = TAG_CREATE;
    }
}

