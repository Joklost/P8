package dk.aau.sw805f18.ar.fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import dk.aau.sw805f18.ar.R;

public class CreateCourseFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
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
    }
}
