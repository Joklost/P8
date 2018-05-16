package dk.aau.sw805f18.ar.argame.location;

import android.support.design.widget.CoordinatorLayout;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.ArSceneView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.OptionalDouble;

import dk.aau.sw805f18.ar.argame.ArGameActivity;
import dk.aau.sw805f18.ar.common.sensor.DeviceLocation;
import dk.aau.sw805f18.ar.common.sensor.DeviceOrientation;
import dk.aau.sw805f18.ar.common.utils.LocationUtils;

public class AugmentedLocationManager {
    private static final String TAG = AugmentedLocationManager.class.getSimpleName();
    // Limit of where to draw markers within AR scene.
    private static final int DISTANCE_LIMIT = 25;

    private final ArrayList<AugmentedLocation> mAugmentedLocations = new ArrayList<>();
    private final ArGameActivity mActivity;
    private final DeviceLocation mDeviceLocation;
    private final DeviceOrientation mDeviceOrientation;

    public AugmentedLocationManager(ArGameActivity activity) {
        mActivity = activity;
        mDeviceLocation = DeviceLocation.getInstance(activity);
        mDeviceOrientation = DeviceOrientation.getInstance(activity);
    }

    private void refreshAnchors(ArSceneView sceneView) {
        for (AugmentedLocation al : mAugmentedLocations) {
            if (al.isLocked()) {
                continue;
            }

            int markerDistance = (int) Math.round(
                    LocationUtils.distance(
                            al.getLocation().getLatitude(),
                            mDeviceLocation.getCurrentBestLocation().getLatitude(),
                            al.getLocation().getLongitude(),
                            mDeviceLocation.getCurrentBestLocation().getLongitude(),
                            0,
                            0
                    )
            );

            Log.i(TAG, "Distance to marker: " + markerDistance);

            // Don't create an anchor if we are not within 25 meters.
            if (markerDistance > DISTANCE_LIMIT) {
                continue;
            }

            double rotation = Math.floor(
                    (mDeviceOrientation.getCurrentDegree()
                            + (float) LocationUtils.bearing(
                            mDeviceLocation.getCurrentBestLocation().getLatitude(),
                            mDeviceLocation.getCurrentBestLocation().getLongitude(),
                            al.getLocation().getLatitude(),
                            al.getLocation().getLongitude()
                    )) % 360
            );

            // When pointing device upwards (camera towards sky)
            // the compass bearing can flip.
            // In experiments this seems to happen at pitch~=-25
            if (mDeviceOrientation.getPitch() > -25) {
                rotation = rotation * Math.PI / 180;
            }

            float x = 0;
            float z = -markerDistance;

            // This makes no sense..
            float zRotated = (float) (z * Math.cos(rotation) - x * Math.sin(rotation));
            float xRotated = (float) -(z * Math.sin(rotation) + x * Math.cos(rotation));

            // Estimate the surface level.
            Collection<Plane> allPlanes = sceneView.getSession().getAllTrackables(Plane.class);
            OptionalDouble avg = allPlanes.stream()
                    .map(Plane::getCenterPose)
                    .map(Pose::ty)
                    .mapToDouble(a -> a)
                    .average();

            if (!avg.isPresent()) {
                Log.e(TAG, "Unable to estimate surface level!");
                continue;
            }

            float y = (float) avg.getAsDouble();
            Pose p = sceneView.getArFrame()
                    .getCamera().getPose().compose(Pose.makeTranslation(xRotated, y, zRotated));

            al.setAnchor(sceneView.getSession().createAnchor(p));
            al.setLocked(true);
            mActivity.addNode(al.getAnchor(), al.getModel());
        }
    }

    public void add(AugmentedLocation locationMarker) {
        mAugmentedLocations.add(locationMarker);
    }

    public void addAll(Collection<AugmentedLocation> locationMarkers) {
        mAugmentedLocations.addAll(locationMarkers);
    }

    public ArrayList<AugmentedLocation> getAugmentedLocations() {
        return mAugmentedLocations;
    }

    public void pause() {
        mDeviceLocation.pause();
        mDeviceOrientation.pause();
    }

    public void resume() {
        mDeviceLocation.resume();
        mDeviceOrientation.resume();
    }

    public void update(ArSceneView sceneView) {
        // No need to add an anchor if we haven't found our location yet.
        if (mDeviceLocation.getCurrentBestLocation() == null) {
            return;
        }

        // We need plane information in order to estimate the height
        // of the Anchors we need to create.
        if (sceneView.getSession().getAllTrackables(Plane.class).size() == 0) {
            return;
        }

        refreshAnchors(sceneView);
    }
}
