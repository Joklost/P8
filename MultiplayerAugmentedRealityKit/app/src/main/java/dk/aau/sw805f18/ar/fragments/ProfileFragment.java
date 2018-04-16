package dk.aau.sw805f18.ar.fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import dk.aau.sw805f18.ar.R;


public class ProfileFragment extends Fragment {
    private boolean mSaveState = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FloatingActionButton editBtn = getView().findViewById(R.id.profile_edit_save_button);
        editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editBtn.setImageDrawable(ContextCompat.getDrawable(getContext(),
                        mSaveState ?
                                R.drawable.ic_edit_black_24dp :
                                R.drawable.ic_save_black_24dp));
                mSaveState = !mSaveState;

                // TODO: make fields editable
                // TODO: actually save changes
                // TODO: populate fields with appropiate information
            }
        });
    }
}
