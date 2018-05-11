package dk.aau.sw805f18.ar.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import com.google.ar.core.HitResult;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.ar.ArActivity;

public class ModelDialogFragment extends DialogFragment {

    private HitResult mHitResult;
    private String[] mModels;

    public void show(FragmentManager manager, String tag, String[] models) {
        mModels = models;
        super.show(manager, tag);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.model_dialog_title)
                .setItems(mModels, (dialog, which) -> {
                    ArActivity arActivity = (ArActivity) getActivity();
                    if (arActivity != null && mHitResult != null) {
                        arActivity.spawnObject(mHitResult.createAnchor(), mModels[which]);
                    }
                });
        return builder.create();
    }

    public void setHitResult(HitResult HitResult) {
        mHitResult = HitResult;
    }
}
