package dk.aau.sw805f18.ar.main;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import dk.aau.sw805f18.ar.R;

public class ModelDialogFragment extends DialogFragment {

    private String[] mModels;
    private int mSelected = -1;

    public void show(FragmentManager manager, String tag, String[] models) {
        super.show(manager, tag);
        mModels = models;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.model_dialog_title)
                .setItems(mModels, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mSelected = which;
                    }
                });
        return builder.create();
    }

    public int getSelected() {
        return mSelected;
    }
}
