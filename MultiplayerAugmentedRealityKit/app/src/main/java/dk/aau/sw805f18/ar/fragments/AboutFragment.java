package dk.aau.sw805f18.ar.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.main.MainActivity;

public class AboutFragment extends Fragment {
    public static final String TAG_ABOUT = "about";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity.CURRENT_FRAGMENT = TAG_ABOUT;
    }
}
