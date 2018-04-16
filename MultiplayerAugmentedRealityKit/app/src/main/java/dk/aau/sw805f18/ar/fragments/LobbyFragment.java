package dk.aau.sw805f18.ar.fragments;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.common.adapters.LobbyGroupAdapter;


public class LobbyFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lobby, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rvGrid = getView().findViewById(R.id.lobby_group_recyclerview);
        LobbyGroupAdapter adapter = new LobbyGroupAdapter();
        rvGrid.setAdapter(adapter);
        rvGrid.setLayoutManager(new GridLayoutManager(getContext(), 3));
    }
}
