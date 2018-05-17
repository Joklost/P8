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

    public AugmentedLocationManager(ArGameActivity activity) {
        mActivity = activity;
        mDeviceLocation = DeviceLocation.getInstance(activity);
        mDeviceOrientation = DeviceOrientation.getInstance(activity);
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

            Log.i(TAG, "Distance to marker: " + markerDistance);

            // Don't create an anchor if we are not within 25 meters.
            if (markerDistance > DISTANCE_LIMIT) {
                continue;
            }

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

            float x = 0;
            float z = -markerDistance;

            // Adjustment to add markers on horizon, instead of just directly in front of camera
            double heightAdjustment = Math.round(markerDistance * (Math.tan(Math.toRadians(mDeviceOrientation.getPitch()))));
            float y = (float) (avg.getAsDouble() + heightAdjustment);

            // Use rotation from Pose in Plane.
            Plane plane = new ArrayList<>(allPlanes).get(0);
            float rotations[] = new float[4];
            plane.getCenterPose().getRotationQuaternion(rotations, 0);
            float translations[] = new float[3];

            sceneView.getArFrame()
                    .getCamera().getDisplayOrientedPose()
                    .compose(Pose.makeTranslation(x, y, z))
                    .getTranslation(translations, 0);

            al.setAnchor(sceneView.getSession().createAnchor(new Pose(translations, rotations)));
            mActivity.addNode(al.getAnchor(), al.getModel());
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
                al.setAnchor(p.createAnchor(anchorPose));
                al.setLocked(true);
                mActivity.addNode(al.getAnchor(), al.getModel());
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
        checkForPlanes(sceneView);
    }

}
