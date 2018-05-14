package dk.aau.sw805f18.ar.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;

import java.util.ArrayList;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.common.adapters.FindCourseListItemAdapter;
import dk.aau.sw805f18.ar.main.FragmentOpener;
import dk.aau.sw805f18.ar.main.MainActivity;
import dk.aau.sw805f18.ar.models.FindCourseItem;


public class FindCourseFragment extends Fragment {
    public static final String TAG_FIND = "findcourse";

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

        distanceAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
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


        ListView lv = getView().findViewById(R.id.find_course_listview);
        FindCourseListItemAdapter lvAdapter = new FindCourseListItemAdapter(getContext(), getCourseItems());
        lv.setAdapter(lvAdapter);

        Button sendBtn = getView().findViewById(R.id.find_course_join_by_code_button);
        sendBtn.setOnClickListener(v -> FragmentOpener.getInstance().open(new LobbyFragment(), TAG_FIND));
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity.CURRENT_FRAGMENT = TAG_FIND;
    }

    private ArrayList<String> getTypeItems() {
        ArrayList<String> typeArray = new ArrayList<>();
        typeArray.add("night event");
        typeArray.add("halloween event");
        typeArray.add("christmas event");
        typeArray.add("sunshine event");

        return typeArray;
    }

    private ArrayList<FindCourseItem> getCourseItems() {
        ArrayList<FindCourseItem> courseArray = new ArrayList<>();
        courseArray.add(new FindCourseItem("Horror map", 7, 14, 3.14));
        courseArray.add(new FindCourseItem("Christmas map", 32, 18, 13.14));
        courseArray.add(new FindCourseItem("ALL YEAR LONG! map", 102, 5, 12.08));
        return  courseArray;
    }
}
