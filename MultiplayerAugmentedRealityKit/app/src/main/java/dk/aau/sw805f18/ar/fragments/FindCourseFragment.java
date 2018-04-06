package dk.aau.sw805f18.ar.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.ArrayList;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.main.MainActivity;
import dk.aau.sw805f18.ar.models.Course;

public class FindCourseFragment extends Fragment {
    private Button mSearchBtn;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_find_course, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSearchBtn = getView().findViewById(R.id.course_search_nearby);
        mSearchBtn.setOnClickListener(view1 -> {
            // Shows progressbar
            ProgressBar pb = getView().findViewById(R.id.course_search_nearby_progressbar);
            pb.setVisibility(View.VISIBLE);
            mSearchBtn.setEnabled(false);

            ArrayList<Course> courses = findNearbyCourses();
            ListView lv = getView().findViewById(R.id.course_search_list);

            // ArrayAdapter for populating listview
            ArrayAdapter<Course> adapter = new ArrayAdapter<>(getContext(), R.layout.list_item_course, courses);
            lv.setAdapter(adapter);

            // Removing progressbar and viewing listview
            pb.setVisibility(View.GONE);
            lv.setVisibility(View.VISIBLE);
            mSearchBtn.setEnabled(true);

            lv.setOnItemClickListener((parent, view2, position, id) -> {
                
            });
        });
    }

    /**
     * Finds courses based on GPS location.
     * TODO: implement GPS functionality. Current version is mock data.
     *
     * @return ArrayList of Courses
     */
    private ArrayList<Course> findNearbyCourses() {
        ArrayList<Course> toReturn = new ArrayList<>();
        toReturn.add(new Course("the cool game"));
        toReturn.add(new Course("the boring game"));
        toReturn.add(new Course("not actually a game"));
        toReturn.add(new Course("the AR game"));
        return toReturn;
    }
}
