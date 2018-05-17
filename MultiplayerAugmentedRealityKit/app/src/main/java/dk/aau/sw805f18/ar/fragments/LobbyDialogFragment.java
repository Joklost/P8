package dk.aau.sw805f18.ar.fragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.common.adapters.SimpleRowAdapter;

public class LobbyDialogFragment extends DialogFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Dialog dialog = getDialog();

        dialog.setTitle("Gruppe navn");
        View layout = inflater.inflate(R.layout.dialog_lobby_content, container, false);

        return layout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rcAssigned = view.findViewById(R.id.dialog_lobby_assigned);
        RecyclerView rcUnassigned = view.findViewById(R.id.dialog_lobby_unassigned);

        ArrayList<String> test_data = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            test_data.add("TROLOLOLOLOL");
        }
        rcAssigned.setAdapter(new SimpleRowAdapter(test_data));
        rcAssigned.setLayoutManager(new LinearLayoutManager(getContext()));
    }
}
