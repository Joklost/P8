package dk.aau.sw805f18.ar.ar;

import android.annotation.SuppressLint;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.common.helpers.CameraPermissionHelper;
import dk.aau.sw805f18.ar.common.helpers.DisplayRotationHelper;
import dk.aau.sw805f18.ar.common.helpers.FullScreenHelper;
import dk.aau.sw805f18.ar.common.helpers.gestures.GestureEvent;
import dk.aau.sw805f18.ar.common.helpers.gestures.LongPress;
import dk.aau.sw805f18.ar.common.helpers.gestures.Scroll;
import dk.aau.sw805f18.ar.common.helpers.SnackbarHelper;
import dk.aau.sw805f18.ar.common.helpers.GestureHelper;
import dk.aau.sw805f18.ar.common.helpers.gestures.Tap;
import dk.aau.sw805f18.ar.common.rendering.BackgroundRenderer;
import dk.aau.sw805f18.ar.common.rendering.PlaneRenderer;
import dk.aau.sw805f18.ar.common.rendering.PointCloudRenderer;
import dk.aau.sw805f18.ar.main.ModelDialogFragment;

public class ArActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
    private static final String TAG = ArActivity.class.getSimpleName();

    // Rendering
    private GLSurfaceView mSurfaceView;

    private boolean mInstallRequested;

    private Session mSession;
    private final SnackbarHelper mMessageSnackbarHelper = new SnackbarHelper();
    private DisplayRotationHelper mDisplayRotationHelper;
    private GestureHelper mGestureHelper;

    // DEBUG: Used to toggle rendering planes
    private ToggleButton mToggle;

    private final String[] ASSETS_TO_LOAD = new String[] {
            "andy",
    };
    private final ArrayList<ArModel> mModels = new ArrayList<>();
    private final ArrayList<ArObject> mObjects = new ArrayList<>();

    private final BackgroundRenderer mBackgroundRenderer = new BackgroundRenderer();
    private final PlaneRenderer mPlaneRenderer = new PlaneRenderer();
    private final PointCloudRenderer mPointCloudRenderer = new PointCloudRenderer();
//    private final ObjectRenderer mVirtualObject = new ObjectRenderer();
//    private final ObjectRenderer mVirtualObjectShadow = new ObjectRenderer();

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final float[] mAnchorMatrix = new float[16];

    private ArObject mSelectedObject;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ar);
        mSurfaceView = findViewById(R.id.surfaceview);
        mDisplayRotationHelper = new DisplayRotationHelper(this);

        mToggle = findViewById(R.id.toggleButton);
//        setSelectedAnchor(null);

        // set up tap listener
        mGestureHelper = new GestureHelper(this);
        mSurfaceView.setOnTouchListener(mGestureHelper);

        // set up renderer
        mSurfaceView.setPreserveEGLContextOnPause(true);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mSurfaceView.setRenderer(this);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        mInstallRequested = false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mSession == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !mInstallRequested)) {
                    case INSTALL_REQUESTED:
                        mInstallRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }

                // create the session
                mSession = new Session(this);
            } catch (UnavailableArcoreNotInstalledException
                    | UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (UnavailableDeviceNotCompatibleException e) {
                message = "This device does not support AR";
                exception = e;
            } catch (Exception e) {
                message = "Failed to create AR session";
                exception = e;
            }

            if (message != null) {
                mMessageSnackbarHelper.showError(this, message);
                Log.e(TAG, "Exception creating session", exception);
            }
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            mSession.resume();
        } catch (CameraNotAvailableException e) {
            // In some cases (such as another camera app launching) the camera may be given to
            // a different app instead. Handle this properly by showing a message and recreate the
            // session at the next iteration.
            mMessageSnackbarHelper.showError(this, "Camera not available. Please restart the app.");
            mSession = null;
            return;
        }

        mSurfaceView.onResume();
        mDisplayRotationHelper.onResume();

        mMessageSnackbarHelper.showMessage(this, "Searching for surfaces...");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSession != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            mDisplayRotationHelper.onPause();
            mSurfaceView.onPause();
            mSession.pause();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        if (CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the texture and pass it to ARCore session to be filled during update().
            mBackgroundRenderer.createOnGlThread(/*context=*/ this);
            mPlaneRenderer.createOnGlThread(/*context=*/ this, "models/trigrid.png");
            mPointCloudRenderer.createOnGlThread(/*context=*/ this);

            for (String asset : ASSETS_TO_LOAD) {
                mModels.add(ModelLoader.load(this, asset));
            }

        } catch (IOException e) {
            Log.e(TAG, "Failed to read an asset file", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mDisplayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
    }

    private boolean hitsPlaneOrPoint(Trackable trackable, HitResult hit) {
        return ((trackable instanceof Plane) && ((Plane) trackable).isPoseInPolygon(hit.getHitPose()))
                || ((trackable instanceof Point)
                && (((Point) trackable).getOrientationMode()
                == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL));
    }

    private float calcPoseDistance(Pose start, Pose end) {
        float dx = start.tx() - end.tx();
        float dy = start.ty() - end.ty();
        float dz = start.tz() - end.tz();

        // Compute the straight-line distance.
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

//    private void setSelectedAnchor(Anchor anchor) {
//        mSelectedAnchor = anchor;
//        final String string;
//        if (mSelectedAnchor == null) {
//            string = "No Anchor selected";
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    findViewById(R.id.rotationBar).setVisibility(View.GONE);
//                }
//            });
//        } else {
//            string = String.valueOf(mSelectedAnchor.getPose());
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    findViewById(R.id.rotationBar).setVisibility(View.VISIBLE);
//                }
//            });
//        }
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                ((TextView) findViewById(R.id.textView)).setText(string);
//            }
//        });
//    }

    private void handleTap(Tap tap, Frame frame) {
        for (HitResult hit : frame.hitTest(tap.getMotion())) {
            Trackable trackable = hit.getTrackable();

            if (!hitsPlaneOrPoint(trackable, hit)) {
                continue;
            }

            Collection<Anchor> trackableAnchors = trackable.getAnchors();
            if (trackableAnchors.isEmpty()) {
                continue;
            }

            Anchor closestAnchor = null;
            float closestDistance = Float.MAX_VALUE;

            for (Anchor anchor : trackableAnchors) {
                float distance = calcPoseDistance(hit.getHitPose(), anchor.getPose());

                if (distance < closestDistance) {
                    closestAnchor = anchor;
                    closestDistance = distance;
                }

                Log.e(TAG, "closestDistance: " + closestDistance);
                Log.e(TAG, "anchorDistance: " + distance);
            }

            if (closestAnchor != null && closestDistance < 0.2f) {
//                setSelectedAnchor(closestAnchor);
            } else {
//                setSelectedAnchor(null);
            }
            return;
        }
//        setSelectedAnchor(null);
    }

    private void handleLongPress(LongPress longPress, Frame frame) {
        for (HitResult hit : frame.hitTest(longPress.getMotion())) {
            // Check if any plane was hit, and if it was hit inside the plane polygon
            Trackable trackable = hit.getTrackable();
            // Creates an anchor if a plane or an oriented point was hit.
            if (!hitsPlaneOrPoint(trackable, hit)) {
                continue;
            }
            final ModelDialogFragment modelDialogFragment = new ModelDialogFragment();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    modelDialogFragment.show(getSupportFragmentManager(), "modelDialog", ASSETS_TO_LOAD);
                }
            });
            Log.e(TAG, String.valueOf(modelDialogFragment.getSelected()));

            // Hits are sorted by depth. Consider only closest hit on a plane or oriented point.
            // Cap the number of objects created. This avoids overloading both the
            // rendering system and ARCore.


//            if (mAnchors.size() >= 10) {
//                mAnchors.get(0).detach();
//                mAnchors.remove(0);
//            }

            // Adding an Anchor tells ARCore that it should track this position in
            // space. This anchor is created on the Plane to place the 3D model
            // in the correct position relative both to the world and to the plane.

            //mObjects.add(new ArObject(hit.createAnchor()))
//            mAnchors.add(hit.createAnchor());
            return;
        }
    }

//    private void handleScroll(Scroll scroll, Frame frame) {
//        if (mSelectedAnchor == null) {
//            return;
//        }
//
//        for (HitResult hit : frame.hitTest(scroll.getMotion())) {
//            Trackable trackable = hit.getTrackable();
//
//            if (!hitsPlaneOrPoint(trackable, hit)) {
//                continue;
//            }
//
//            if (calcPoseDistance(mSelectedAnchor.getPose(), hit.getHitPose()) >= 0.2f) {
//                return;
//            }
//
//            mAnchors.remove(mSelectedAnchor);
//
//            for (HitResult moveHit : frame.hitTest(scroll.getCurrentMove())) {
//                Trackable moveTrackable = moveHit.getTrackable();
//
//                if (!hitsPlaneOrPoint(moveTrackable, moveHit)) {
//                    continue;
//                }
//
//                Log.e(TAG, "PLANE/POINT MOVE: " + moveHit.getHitPose());
//                Anchor anchor = moveHit.createAnchor();
//                mAnchors.add(anchor);
//                setSelectedAnchor(anchor);
//                break;
//            }
//        }
//    }

    private void handleGesture(GestureEvent gestureEvent, Frame frame, Camera camera) {
        if (camera.getTrackingState() != TrackingState.TRACKING) {
            return;
        }

        if (gestureEvent instanceof Tap) {
            handleTap((Tap) gestureEvent, frame);
        } else if (gestureEvent instanceof LongPress) {
            handleLongPress((LongPress) gestureEvent, frame);
        } else if (gestureEvent instanceof Scroll) {
//            handleScroll((Scroll) gestureEvent, frame);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (mSession == null) {
            return;
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        mDisplayRotationHelper.updateSessionIfNeeded(mSession);

        try {
            mSession.setCameraTextureName(mBackgroundRenderer.getTextureId());

            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            Frame frame = mSession.update();
            Camera camera = frame.getCamera();

            // Handle gestures. Handling only one gesture per frame, as gestures are usually low
            // frequency, compared to frame rate.
            GestureEvent gestureEvent = mGestureHelper.poll();
            if (gestureEvent != null) {
                handleGesture(gestureEvent, frame, camera);
            }

            // Draw background.
            mBackgroundRenderer.draw(frame);

            // If not tracking, don't draw 3d objects.
            if (camera.getTrackingState() == TrackingState.PAUSED) {
                return;
            }

            // Get projection matrix.
            float[] projmtx = new float[16];
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            // Get camera matrix and draw.
            float[] viewmtx = new float[16];
            camera.getViewMatrix(viewmtx, 0);

            // Compute lighting from average intensity of the image.
            // The first three components are color scaling factors.
            // The last one is the average pixel intensity in gamma space.
            final float[] colorCorrectionRgba = new float[4];
            frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

            // Visualize tracked points.
            PointCloud pointCloud = frame.acquirePointCloud();
            mPointCloudRenderer.update(pointCloud);
            mPointCloudRenderer.draw(viewmtx, projmtx);

            // Application is responsible for releasing the point cloud resources after
            // using it.
            pointCloud.release();

            // Check if we detected at least one plane. If so, hide the loading message.
            if (mMessageSnackbarHelper.isShowing()) {
                for (Plane plane : mSession.getAllTrackables(Plane.class)) {
                    if (plane.getType() == com.google.ar.core.Plane.Type.HORIZONTAL_UPWARD_FACING
                            && plane.getTrackingState() == TrackingState.TRACKING) {
                        mMessageSnackbarHelper.hide(this);
                        break;
                    }
                }
            }

            // Visualize planes.
            if (mToggle.isChecked()) {
                mPlaneRenderer.drawPlanes(
                        mSession.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projmtx);

            }

//            // Visualize anchors created by touch.
//            float scaleFactor = 1.0f;
//            for (Anchor anchor : mAnchors) {
//                if (anchor.getTrackingState() != TrackingState.TRACKING) {
//                    continue;
//                }
//                // Get the current pose of an Anchor in world space. The Anchor pose is updated
//                // during calls to session.update() as ARCore refines its estimate of the world.
//                anchor.getPose().toMatrix(mAnchorMatrix, 0);
//
//                // Update and draw the model and its shadow.
//                mVirtualObject.updateModelMatrix(mAnchorMatrix, scaleFactor);
//                mVirtualObjectShadow.updateModelMatrix(mAnchorMatrix, scaleFactor);
//                mVirtualObject.draw(viewmtx, projmtx, colorCorrectionRgba);
//                mVirtualObjectShadow.draw(viewmtx, projmtx, colorCorrectionRgba);
//            }

        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }
}
