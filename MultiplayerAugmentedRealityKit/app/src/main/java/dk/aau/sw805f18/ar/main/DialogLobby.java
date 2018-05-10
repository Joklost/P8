package dk.aau.sw805f18.ar.main;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.common.adapters.DialogLobbyAssignedAdapter;

public class DialogLobby extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.dialog_lobby_content, null))
                .setTitle("Gruppe navn")
                .setPositiveButton("Add", (dialog, id) -> {

                })
                .setNegativeButton("Cancel", (dialog, id) -> {

                });

        RecyclerView rcAssigned = getView().findViewById(R.id.dialog_lobby_assigned);
        RecyclerView rcUnassigned = getView().findViewById(R.id.dialog_lobby_unassigned);

        rcAssigned.setAdapter(new DialogLobbyAssignedAdapter());
        rcAssigned.setLayoutManager(new LinearLayoutManager(getContext()));

        return builder.create();
    }
}
