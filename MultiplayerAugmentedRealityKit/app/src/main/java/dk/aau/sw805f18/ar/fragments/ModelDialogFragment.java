package dk.aau.sw805f18.ar.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.argame.Model;

public class ModelDialogFragment extends DialogFragment {

    private List<Model> mModels;
    private Consumer<String> mOnPick;

    public void show(FragmentManager manager, String tag, HashMap<String, Model> models, Consumer<String> onPick) {
        mModels = new ArrayList<>(models.values());
        mOnPick = onPick;
        super.show(manager, tag);
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        List<String> options = new ArrayList<>();
        for (Model model : mModels) {
            options.add(model.getTitle());
        }

        builder.setTitle(R.string.model_dialog_title).setItems(options.toArray(new String[0]), (dialog, which) -> {
            mOnPick.accept(options.get(which));
        });

        return builder.create();
    }

}
