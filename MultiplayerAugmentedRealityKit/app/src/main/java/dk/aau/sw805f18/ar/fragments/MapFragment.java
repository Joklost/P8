package dk.aau.sw805f18.ar.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import dk.aau.sw805f18.ar.R;

public class MapFragment extends Fragment implements OnMapReadyCallback {
    public static final String TAG_MAP = "map";

    private GoogleMap googleMap;
    MapView mMapView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_map, container, false);
        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMapView = rootView.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume();
        mMapView.getMapAsync(this);
        return rootView;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //kan spørge efter permisions, måske en future TODO
//                return;
//            googleMap.setMyLocationEnabled(true);
        }

        LatLng cass = new LatLng(57.013234, 9.991280);
        LatLngBounds cass_bounds = new LatLngBounds(cass, new LatLng(57.020992, 9.977574));

        // Set marker on cass and move camera
        googleMap.addMarker(new MarkerOptions().position(cass).title("Marker Title").snippet("Marker Description"));
        CameraPosition cameraPosition = new CameraPosition.Builder().target(cass).zoom(15).build();
        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        // Set bounds to cass area
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cass_bounds.getCenter(), 10));

//        googleMap.setLatLngBoundsForCameraTarget(cass_bounds);
//        googleMap.setMaxZoomPreference(12.0f);
//        googleMap.setMinZoomPreference(12.0f);


    }
}

