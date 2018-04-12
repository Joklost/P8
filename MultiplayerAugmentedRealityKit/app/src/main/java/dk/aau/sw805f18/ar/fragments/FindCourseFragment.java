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

import java.util.ArrayList;

import dk.aau.sw805f18.ar.R;


public class FindCourseFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_find_course, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Spinner distance = getView().findViewById(R.id.find_course_distance_spinner);
        Spinner type = getView().findViewById(R.id.find_course_type_spinner);
        Spinner age = getView().findViewById(R.id.find_course_age_spinner);

        ArrayAdapter<CharSequence> distanceAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.find_course_distance_spinner_array,
                android.R.layout.simple_spinner_item);

        distanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        distance.setAdapter(distanceAdapter);

        ArrayAdapter<CharSequence> ageAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.find_course_age_spinner_array,
                android.R.layout.simple_spinner_item);

        ageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        age.setAdapter(ageAdapter);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                getTypeItems());

        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        type.setAdapter(typeAdapter);
    }

    private ArrayList<String> getTypeItems() {
        ArrayList<String> typeArray = new ArrayList<>();
        typeArray.add("night event");
        typeArray.add("halloween event");
        typeArray.add("christmas event");
        typeArray.add("sunshine event");

        return typeArray;
    }
}
