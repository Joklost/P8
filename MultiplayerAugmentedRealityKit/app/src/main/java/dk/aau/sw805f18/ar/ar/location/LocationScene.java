package dk.aau.sw805f18.ar.ar.location;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.OptionalDouble;

import dk.aau.sw805f18.ar.ar.ArActivity;
import dk.aau.sw805f18.ar.ar.location.sensor.DeviceLocation;
import dk.aau.sw805f18.ar.ar.location.sensor.DeviceOrientation;
import dk.aau.sw805f18.ar.ar.location.utils.LocationUtils;


public class LocationScene {
    private static final String TAG = LocationScene.class.getSimpleName();

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final float[] mAnchorMatrix = new float[16];
    private ArrayList<LocationMarker> mLocationMarkers = new ArrayList<>();

    private DeviceLocation mDeviceLocation;
    private DeviceOrientation mDeviceOrientation;

    // Limit of where to draw markers within AR scene.
    // They will auto scale, but this helps prevents rendering issues
    private int mDistanceLimit = 50;

    // Bearing adjustment. Can be set to calibrate with true north
    private int mBearingAdjustment = 0;

    private ArActivity mActivity;

    public LocationScene(ArActivity activity) {
        mActivity = activity;
        mDeviceLocation = DeviceLocation.getInstance(activity);
        mDeviceOrientation = new DeviceOrientation(activity);
    }

    public void draw(Session session, Frame frame) {
        // Refresh the anchors in the scene.
        // Needs to occur in the draw method, as we need details about the camera
        refreshAnchors(session, frame);

        // check if the anchors in each LocationMarker is above a Plane,
        // and if so, add the anchor to the plane and lock the LocationMarker.
        checkForPlanes(session);

        // Draw each anchor with it's individual renderer.
        drawMarkers(frame);
    }

    private void drawMarkers(Frame frame) {
        for (LocationMarker marker : mLocationMarkers) {
            if (marker.isLocked()) {
                continue;
            }
            if (marker.getAnchor() == null) {
                return;
            }

            // Get the current pose of an Anchor in world space. The Anchor pose is updated
            // during calls to session.update() as ARCore refines its estimate of the world.
            float translation[] = new float[3];
            float rotation[] = new float[4];
            marker.getAnchor().getPose().getTranslation(translation, 0);
            frame.getCamera().getPose().getRotationQuaternion(rotation, 0);

            Pose rotatedPose = new Pose(translation, rotation);
            rotatedPose.toMatrix(mAnchorMatrix, 0);

            int markerDistance = (int) Math.ceil(
                    LocationUtils.distance(
                            marker.getLocation().getLatitude(),
                            mDeviceLocation.getCurrentBestLocation().getLatitude(),
                            marker.getLocation().getLongitude(),
                            mDeviceLocation.getCurrentBestLocation().getLongitude(),
                            0,
                            0)
            );

            // Limit the distance of the Anchor within the scene.
            // Prevents uk.co.appoly.arcorelocation.rendering issues.
            int renderDistance = markerDistance;
            if (renderDistance > mDistanceLimit)
                renderDistance = mDistanceLimit;


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
            marker.getRenderer().updateModelMatrix(mAnchorMatrix, scale, 0);
            marker.getRenderer().draw(viewMatrix, projectionMatrix, colorCorrectionRgba, lightIntensity);
        }
    }

    private void checkForPlanes(Session session) {
        for (LocationMarker marker : mLocationMarkers) {
            if (marker.isLocked()) {
                continue;
            }
            for (Plane p : session.getAllTrackables(Plane.class)) {
                Pose pose = marker.getAnchor().getPose().extractTranslation();

                if (!p.isPoseInPolygon(pose)) {
                    continue;
                }

                pose = pose.compose(Pose.makeTranslation(0, shiftYAxisBy(p.getCenterPose().ty(), pose.ty()), 0));
                removeAnchor(session, marker.getAnchor());
                marker.setLocked(true);
                marker.setAnchor(p.createAnchor(pose));
                // TODO: Change modelName, so that it is not hardcoded in.
                mActivity.spawnObject(marker.getAnchor(), "rabbit");
            }
        }
    }

    private void refreshAnchors(Session session, Frame frame) {
        for (LocationMarker marker : mLocationMarkers) {
            if (marker.isLocked()) {
                continue;
            }

            if (marker.getAnchor() != null) {
                continue;
            }

            int markerDistance = (int) Math.round(
                    LocationUtils.distance(
                            marker.getLocation().getLatitude(),
                            mDeviceLocation.getCurrentBestLocation().getLatitude(),
                            marker.getLocation().getLongitude(),
                            mDeviceLocation.getCurrentBestLocation().getLongitude(),
                            0,
                            0)
            );

            float markerBearing = mDeviceOrientation.currentDegree + (float) LocationUtils.bearing(
                    mDeviceLocation.getCurrentBestLocation().getLatitude(),
                    mDeviceLocation.getCurrentBestLocation().getLongitude(),
                    marker.getLocation().getLatitude(),
                    marker.getLocation().getLongitude());

            // Bearing adjustment can be set if you are trying to
            // correct the heading of north - setBearingAdjustment(10)
            markerBearing = markerBearing + mBearingAdjustment;
            markerBearing = markerBearing % 360;

            double rotation = Math.floor(markerBearing);

            // When pointing device upwards (camera towards sky)
            // the compass bearing can flip.
            // In experiments this seems to happen at pitch~=-25
            if (mDeviceOrientation.pitch > -25)
                rotation = rotation * Math.PI / 180;

            int renderDistance = markerDistance;

            // Limit the distance of the Anchor within the scene.
            // Prevents rendering issues.
            if (renderDistance > mDistanceLimit) {
                renderDistance = mDistanceLimit;
            }

            // Adjustment to add markers on horizon, instead of just directly in front of camera
            double heightAdjustment = Math.round(renderDistance * (Math.tan(Math.toRadians(mDeviceOrientation.pitch))));
            // Raise distant markers for better illusion of distance
            // Hacky - but it works as a temporary measure
            int cappedRealDistance = markerDistance > 500 ? 500 : markerDistance;
            if (renderDistance != markerDistance) {
                heightAdjustment += 0.01F * (cappedRealDistance - renderDistance);
            }

            float x = 0;
            float z = -renderDistance;

            float zRotated = (float) (z * Math.cos(rotation) - x * Math.sin(rotation));
            float xRotated = (float) -(z * Math.sin(rotation) + x * Math.cos(rotation));

            // Approximate the surface level
            Collection<Plane> allTrackables = session.getAllTrackables(Plane.class);
            List<Float> levels = new ArrayList<>();

            for (Plane p : allTrackables) {
                levels.add(p.getCenterPose().ty());
            }

            OptionalDouble average = levels
                    .stream()
                    .mapToDouble(a -> a)
                    .average();
            float y = average.isPresent() ? (float) average.getAsDouble() : frame.getCamera().getDisplayOrientedPose().ty();

            Pose newPose = frame.getCamera().getPose()
                    .compose(Pose.makeTranslation(xRotated, y, zRotated));

            // TODO: Might not be needed
            removeAnchor(session, marker.getAnchor());
            marker.setAnchor(session.createAnchor(newPose));
            try {
                marker.getRenderer().createOnGlThread(mActivity, markerDistance);
            } catch (IOException ignore) {}
        }
    }

    private float shiftYAxisBy(double planeAnchor, double airAnchor) {
        return (float) (planeAnchor - airAnchor);
    }

    private void removeAnchor(Session session, Anchor anchor) {
        for (Anchor a : session.getAllAnchors()) {
            if (a.equals(anchor)) {
                a.detach();
            }
        }
    }

    public int getBearingAdjustment() {
        return mBearingAdjustment;
    }

    public void setBearingAdjustment(int i) {
        mBearingAdjustment = i;
    }

    public void add(LocationMarker marker) {
        mLocationMarkers.add(marker);
    }

    public void addAll(Collection<LocationMarker> markers) {
        mLocationMarkers.addAll(markers);
    }

    public void resume() {
        mDeviceLocation.resume();
        mDeviceOrientation.resume();
    }

    public void pause() {
        mDeviceLocation.pause();
        mDeviceOrientation.pause();
    }
}
