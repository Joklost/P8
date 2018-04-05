package dk.aau.sw805f18.ar.main;

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
                .setItems(mModels, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ArActivity arActivity = (ArActivity) getActivity();
                        if (arActivity != null && mHitResult != null) {
                            arActivity.spawnObject(mHitResult, mModels[which]);
                        }
                    }
                });
        return builder.create();
    }

    public static ModelDialogFragment newInstance(int title) {
        ModelDialogFragment frag = new ModelDialogFragment();
        Bundle args = new Bundle();
        args.putInt("title", title);
        frag.setArguments(args);
        return frag;
    }

    public void setHitResult(HitResult HitResult) {
        mHitResult = HitResult;
    }
}
