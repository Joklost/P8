package dk.aau.sw805f18.ar.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.ar.ArActivity;
import dk.aau.sw805f18.ar.common.helpers.SyncServiceHelper;
import dk.aau.sw805f18.ar.common.websocket.Packet;
import dk.aau.sw805f18.ar.models.Marker;

public class MapFragment extends Fragment implements OnMapReadyCallback {
    public static final String TAG = MapFragment.class.getSimpleName();
    public static final String TAG_MAP = "map";
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

        mMapView = rootView.findViewById(R.id.map_map_widget);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume();
        mMapView.getMapAsync(this);
        return rootView;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //kan spørge efter permisions, måske en future TODO
//                return;
            googleMap.setMyLocationEnabled(true);
        }

        SyncServiceHelper.getInstance().getWebSocket().send(new Packet(Packet.OBJECTS_TYPE, ""));

        SyncServiceHelper.getInstance().getWebSocket().attachHandler(Packet.OBJECTS_TYPE, packet -> {
            Gson gson = new Gson();
            List<Marker> markers = gson.fromJson(packet.Data, new TypeToken<ArrayList<Marker>>() {
            }.getType());

            boolean firstEl = true;
            for (Marker marker : markers) {
                LatLng location = new LatLng(marker.Location.Lat, marker.Location.Lon);

                if (firstEl) {
                    // Lock the map, with the first marker as the center
                    CameraPosition cameraPosition = new CameraPosition.Builder().target(location).zoom(17).build();
                    getActivity().runOnUiThread(() -> {
                        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                        googleMap.getUiSettings().setZoomGesturesEnabled(false);
                        googleMap.getUiSettings().setScrollGesturesEnabled(false);
                        googleMap.getUiSettings().setRotateGesturesEnabled(false);
                        googleMap.getUiSettings().setTiltGesturesEnabled(false);
                    });

                    firstEl = false;
                }

                // Add a marker to the map
                getActivity().runOnUiThread(() -> {
                    googleMap.addMarker(new MarkerOptions().position(location));
                });
            }

            SyncServiceHelper.getInstance().getWebSocket().removeHandler(Packet.OBJECTS_TYPE);
        });

        //TODO: Call when within a control post's range
//        gameFoundPopup();

    }

    private void gameFoundPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        getActivity().runOnUiThread(() -> {
            builder.setMessage("Do you want to start?")
                    .setPositiveButton("Start", ((dialog, which) -> {
                        startActivity(new Intent(getContext(), ArActivity.class));

                    }))
                    .setNegativeButton("Cancel", ((dialog, which) -> {

                    }))
                    .show();
        });
    }
}

