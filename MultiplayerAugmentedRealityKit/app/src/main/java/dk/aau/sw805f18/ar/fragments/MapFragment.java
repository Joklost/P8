package dk.aau.sw805f18.ar.fragments;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.databinding.FragmentMapBinding;

public class MapFragment extends Fragment {
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FragmentMapBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_map, container, false);
        return binding.getRoot();
    }
}
