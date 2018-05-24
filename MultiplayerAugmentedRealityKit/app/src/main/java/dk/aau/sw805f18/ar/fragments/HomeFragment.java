package dk.aau.sw805f18.ar.fragments;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.main.FragmentOpener;
import dk.aau.sw805f18.ar.main.MainActivity;


public class HomeFragment extends Fragment {
    public static final String TAG_HOME = "home";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button findBtn = getView().findViewById(R.id.home_find_course_button);
        Button createBtn = getView().findViewById(R.id.home_create_course_button);

        findBtn.setOnClickListener(v -> FragmentOpener.getInstance().open(new FindCourseFragment(), FindCourseFragment.TAG_FIND));

        createBtn.setOnClickListener(v -> FragmentOpener.getInstance().open(new CreateCourseFragment(), CreateCourseFragment.TAG_CREATE));

    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity.CURRENT_FRAGMENT = TAG_HOME;
    }

}
