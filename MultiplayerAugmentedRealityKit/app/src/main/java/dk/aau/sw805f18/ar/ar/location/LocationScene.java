package dk.aau.sw805f18.ar.ar.location;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.OptionalDouble;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.ar.ArActivity;
import dk.aau.sw805f18.ar.ar.location.sensor.DeviceLocation;
import dk.aau.sw805f18.ar.ar.location.sensor.DeviceOrientation;
import dk.aau.sw805f18.ar.ar.location.utils.LocationUtils;


/**
 * Created by John on 02/03/2018.
 */

public class LocationScene {

    // Anchors are currently re-drawn on an interval. There are likely better
    // ways of doing this, however it's sufficient for now.
    private final static int ANCHOR_REFRESH_INTERVAL = 1000 * 8; // 8 seconds
    public static Context mContext;
    public static Activity mActivity;

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final float[] mAnchorMatrix = new float[16];
    public ArrayList<LocationMarker> mLocationMarkers = new ArrayList<>();

    public DeviceLocation deviceLocation;
    public DeviceOrientation deviceOrientation;

    // Limit of where to draw markers within AR scene.
    // They will auto scale, but this helps prevents rendering issues
    private int distanceLimit = 50;

    // Bearing adjustment. Can be set to calibrate with true north
    private int bearingAdjustment = 0;
    private Location mCachedLocation;

    private String TAG = "LocationScene";
    private boolean anchorsNeedRefresh = false;
    private Handler mHandler = new Handler();

    public void refreshAnchors() {
        anchorsNeedRefresh = true;
    }

    private Runnable anchorRefreshTask = new Runnable() {
        @Override
        public void run() {

            if (deviceLocation != null && deviceLocation.getCurrentBestLocation() != null && mCachedLocation == null) {
//                anchorsNeedRefresh = true;
                mCachedLocation = deviceLocation.getCurrentBestLocation();
            } else if (deviceLocation != null && deviceLocation.getCurrentBestLocation() != null &&
                    LocationUtils.distance(deviceLocation.getCurrentBestLocation().getLatitude(),
                            mCachedLocation.getLatitude(),
                            deviceLocation.getCurrentBestLocation().getLongitude(),
                            mCachedLocation.getLongitude(),
                            0,
                            0) > 5) {
//                anchorsNeedRefresh = true;
                mCachedLocation = deviceLocation.getCurrentBestLocation();
            }
            mHandler.postDelayed(anchorRefreshTask, ANCHOR_REFRESH_INTERVAL);
        }
    };
    private Session mSession;

    public LocationScene(Context mContext, Activity mActivity, Session mSession) {
        this.mContext = mContext;
        this.mActivity = mActivity;
        this.mSession = mSession;

        startCalculationTask();

        deviceLocation = new DeviceLocation(mActivity);
        deviceOrientation = new DeviceOrientation();
    }


    public void draw(Frame frame) {

        // Refresh the anchors in the scene.
        // Needs to occur in the draw method, as we need details about the camera
        refreshAnchorsIfRequired(frame);

        // Draw each anchor with it's individual renderer.
        drawMarkers(frame);

//        for (LocationMarker lm : mLocationMarkers) {
//            if (lm.anchor == null) {
//                anchorsNeedRefresh = true;
//            }
//        }
//
//        if (frame.getCamera().getTrackingState() != TrackingState.TRACKING)
//            anchorsNeedRefresh = true;
    }

    public void drawMarkers(Frame frame) {
        for (LocationMarker locationMarker : mLocationMarkers) {

            try {
                // Get the current pose of an Anchor in world space. The Anchor pose is updated
                // during calls to session.update() as ARCore refines its estimate of the world.

                float translation[] = new float[3];
                float rotation[] = new float[4];
                if (locationMarker.anchor == null) {
                    return;
                }
                locationMarker.anchor.getPose().getTranslation(translation, 0);
                frame.getCamera().getPose().getRotationQuaternion(rotation, 0);

                Pose rotatedPose = new Pose(translation, rotation);
                rotatedPose.toMatrix(mAnchorMatrix, 0);

                int markerDistance = (int) Math.ceil(
                        LocationUtils.distance(
                                locationMarker.latitude,
                                deviceLocation.getCurrentBestLocation().getLatitude(),
                                locationMarker.longitude,
                                deviceLocation.getCurrentBestLocation().getLongitude(),
                                0,
                                0)
                );

                // Limit the distance of the Anchor within the scene.
                // Prevents uk.co.appoly.arcorelocation.rendering issues.
                int renderDistance = markerDistance;
                if (renderDistance > distanceLimit)
                    renderDistance = distanceLimit;


                float[] projectionMatrix = new float[16];
                frame.getCamera().getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f);

                // Get camera matrix and draw.
                float[] viewMatrix = new float[16];
                frame.getCamera().getViewMatrix(viewMatrix, 0);

                // Make sure marker stays the same size on screen, no matter the distance
                float scale = 3.0F / 10.0F * (float) renderDistance;

                // Distant markers a little smaller
                if (markerDistance > 3000)
                    scale *= 0.75F;

                // Compute lighting from average intensity of the image.
                final float lightIntensity = frame.getLightEstimate().getPixelIntensity();

                // Compute lighting from average intensity of the image.
                // The first three components are color scaling factors.
                // The last one is the average pixel intensity in gamma space.
                final float[] colorCorrectionRgba = new float[4];
                frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);
                locationMarker.renderer.updateModelMatrix(mAnchorMatrix, scale, 0);
                locationMarker.renderer.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, lightIntensity);


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void refreshAnchorsIfRequired(Frame frame) {
        if (anchorsNeedRefresh) {
            anchorsNeedRefresh = false;

            for (int i = 0; i < mLocationMarkers.size(); i++) {
                try {

                    int markerDistance = (int) Math.round(
                            LocationUtils.distance(
                                    mLocationMarkers.get(i).latitude,
                                    deviceLocation.getCurrentBestLocation().getLatitude(),
                                    mLocationMarkers.get(i).longitude,
                                    deviceLocation.getCurrentBestLocation().getLongitude(),
                                    0,
                                    0)
                    );

                    float markerBearing = deviceOrientation.currentDegree + (float) LocationUtils.bearing(
                            deviceLocation.getCurrentBestLocation().getLatitude(),
                            deviceLocation.getCurrentBestLocation().getLongitude(),
                            mLocationMarkers.get(i).latitude,
                            mLocationMarkers.get(i).longitude);

                    // Bearing adjustment can be set if you are trying to
                    // correct the heading of north - setBearingAdjustment(10)
                    markerBearing = markerBearing + bearingAdjustment;
                    markerBearing = markerBearing % 360;

                    double rotation = Math.floor(markerBearing);

                    // When pointing device upwards (camera towards sky)
                    // the compass bearing can flip.
                    // In experiments this seems to happen at pitch~=-25
                    if (deviceOrientation.pitch > -25)
                        rotation = rotation * Math.PI / 180;

                    int renderDistance = markerDistance;

                    // Limit the distance of the Anchor within the scene.
                    // Prevents rendering issues.
                    if (renderDistance > distanceLimit)
                        renderDistance = distanceLimit;

                    // Adjustment to add markers on horizon, instead of just directly in front of camera
                    double heightAdjustment = Math.round(renderDistance * (Math.tan(Math.toRadians(deviceOrientation.pitch))));
                    // Raise distant markers for better illusion of distance
                    // Hacky - but it works as a temporary measure
                    int cappedRealDistance = markerDistance > 500 ? 500 : markerDistance;
                    if (renderDistance != markerDistance)
                        heightAdjustment += 0.01F * (cappedRealDistance - renderDistance);

                    float x = 0;
                    float z = -renderDistance;

                    float zRotated = (float) (z * Math.cos(rotation) - x * Math.sin(rotation));
                    float xRotated = (float) -(z * Math.sin(rotation) + x * Math.cos(rotation));

                    // Approximate the surface level
                    Collection<Plane> allTrackables = mSession.getAllTrackables(Plane.class);
                    List<Float> levels = new ArrayList<>();
                    Plane plane = null;
                    for (Plane p : allTrackables) {
                        float y = p.getCenterPose().ty();
                        if (p.isPoseInPolygon(frame.getCamera().getPose()
                                .compose(Pose.makeTranslation(xRotated, y, zRotated)))) {
                            Log.i(TAG, "Pose is in Polygon");
                            plane = p;
                            mActivity.runOnUiThread(() -> {
                                TextView t = mActivity.findViewById(R.id.logText);
                                t.setText("Found Pose over Polygon");
                            });
                        }
                        levels.add(p.getCenterPose().ty());
                    }
                    OptionalDouble average = levels
                            .stream()
                            .mapToDouble(a -> a)
                            .average();
                    float y = average.isPresent() ? (float) average.getAsDouble() : frame.getCamera().getDisplayOrientedPose().ty();

                    // Current camera height
//                    float y = frame.getCamera().getDisplayOrientedPose().ty();
                    float finalMarkerBearing = markerBearing;
                    int finalRenderDistance = renderDistance;
                    mActivity.runOnUiThread(() -> {
                        ((ArActivity) mActivity).fillDebugText(deviceLocation.getCurrentBestLocation(), deviceOrientation.currentDegree, finalMarkerBearing, markerDistance, finalRenderDistance);
                    });
                    // Don't immediately assign newly created anchor in-case of exceptions
//                    Collection<Plane> allTrackables = mSession.getAllTrackables(Plane.class);
//                    for (Plane p : allTrackables) {
//                        p.
//                    }

                    Pose newPose = frame.getCamera().getPose()
                            .compose(Pose.makeTranslation(xRotated, (float) y, zRotated));

                    Anchor newAnchor = mSession.createAnchor(newPose);

                    mLocationMarkers.get(i).anchor = newAnchor;

                    mLocationMarkers.get(i).renderer.createOnGlThread(mContext, markerDistance);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }


    public int getBearingAdjustment() {
        return bearingAdjustment;
    }

    public void setBearingAdjustment(int i) {
        bearingAdjustment = i;
        anchorsNeedRefresh = true;
    }

    public void resume() {
        deviceLocation.resume();
        deviceOrientation.resume();
    }

    public void pause() {
        deviceLocation.pause();
        deviceOrientation.pause();
    }

    private void startCalculationTask() {
        anchorRefreshTask.run();
    }

    void stopCalculationTask() {
        mHandler.removeCallbacks(anchorRefreshTask);
    }
}
