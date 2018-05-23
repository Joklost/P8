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
import java.util.stream.Collectors;

import dk.aau.sw805f18.ar.R;
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

    private boolean mPaused;

    public AugmentedLocationManager(ArGameActivity activity) {
        mActivity = activity;
        mDeviceLocation = DeviceLocation.getInstance(activity);
        mDeviceOrientation = DeviceOrientation.getInstance(activity);
    }

    private static float calcPlaneLevel(Collection<Plane> planes) {

        // Estimate the surface level.
        OptionalDouble avg = planes.stream()
                .map(Plane::getCenterPose)
                .map(Pose::ty)
                .mapToDouble(a -> a)
                .max();

        if (!avg.isPresent()) {
            return Float.MIN_VALUE;
        }

        return (float) avg.getAsDouble();
    }

    private void refreshAnchors(ArSceneView sceneView) {
        for (AugmentedLocation al : mAugmentedLocations) {
            if (al.isLocked() || al.getAnchor() != null) {
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

            // Don't create an anchor if we are not within 25 meters.
            if (markerDistance > DISTANCE_LIMIT) {
                continue;
            }

            Log.i(TAG, "Distance to marker: " + markerDistance);

            float markerBearing = mDeviceOrientation.getCurrentDegree() + (float) LocationUtils.bearing(
                    mDeviceLocation.getCurrentBestLocation().getLatitude(),
                    mDeviceLocation.getCurrentBestLocation().getLongitude(),
                    al.getLocation().getLatitude(),
                    al.getLocation().getLongitude());

            double bearing = Math.floor(markerBearing);

            Collection<Plane> planes = sceneView.getSession().getAllTrackables(Plane.class);

            Log.i(TAG,  "number of planes" + planes.size());
            float y = calcPlaneLevel(planes);
            float x = (float) -(-markerDistance * Math.sin(bearing));
            float z = (float) (-markerDistance * Math.cos(bearing));

            if (y == Float.MIN_VALUE) {
                continue;
            }

            // Use rotation from Pose in Plane.
            Plane plane = new ArrayList<>(planes).get(0);
            float rotations[] = new float[4];
            plane.getCenterPose().getRotationQuaternion(rotations, 0);
            float translations[] = new float[]{x, y, z};

            Anchor anchor = mActivity.addNode(sceneView.getSession().createAnchor(new Pose(translations, rotations)), al.getId(), al.getModel());

            al.setAnchor(anchor);
        }
    }

    private void checkForPlanes(ArSceneView sceneView) {
        for (AugmentedLocation al : mAugmentedLocations) {
            if (al.isLocked() || al.getAnchor() == null) {
                continue;
            }

            Pose anchorPose = al.getAnchor().getPose();
            for (Plane p : sceneView.getSession().getAllTrackables(Plane.class)) {
                if (!p.isPoseInPolygon(anchorPose)) {
                    continue;
                }

                anchorPose = anchorPose.compose(
                        Pose.makeTranslation(
                                0,
                                p.getCenterPose().ty() - anchorPose.ty(),
                                0
                        )
                );

                al.getAnchor().detach();
                Anchor anchor = mActivity.addNode(p.createAnchor(anchorPose), al.getId(), al.getModel());
                al.setAnchor(anchor);
                al.setLocked(true);
                break;
            }
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
        mPaused = true;
        mDeviceLocation.pause();
        mDeviceOrientation.pause();
    }

    public void resume() {
        mPaused = false;
        mDeviceLocation.resume();
        mDeviceOrientation.resume();
    }

    public void onUpdate(ArSceneView sceneView) {
        if (mPaused) {
            return;
        }

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
        checkForPlanes(sceneView);
    }

}
