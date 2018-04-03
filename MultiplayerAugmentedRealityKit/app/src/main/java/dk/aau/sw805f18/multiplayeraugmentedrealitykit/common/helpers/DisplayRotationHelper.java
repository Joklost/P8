package dk.aau.sw805f18.multiplayeraugmentedrealitykit.common.helpers;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.view.WindowManager;

import com.google.ar.core.Session;

import java.util.Objects;

public class DisplayRotationHelper implements DisplayManager.DisplayListener {
    private boolean viewportChanged;
    private int viewportWidth;
    private int viewportHeight;
    private final Context context;
    private final Display display;

    public DisplayRotationHelper(Context context) {
        this.context = context;
        display = Objects.requireNonNull(context.getSystemService(WindowManager.class)).getDefaultDisplay();
    }

    public void onResume() {
        Objects.requireNonNull(context.getSystemService(DisplayManager.class)).registerDisplayListener(this, null);
    }

    public void onPause() {
        Objects.requireNonNull(context.getSystemService(DisplayManager.class)).unregisterDisplayListener(this);
    }

    public void onSurfaceChanged(int width, int height) {
        viewportWidth = width;
        viewportHeight = height;
        viewportChanged = true;
    }

    public void updateSessionIfNeeded(Session session) {
        if (viewportChanged) {
            int displayRotation = display.getRotation();
            session.setDisplayGeometry(displayRotation, viewportWidth, viewportHeight);
            viewportChanged = false;
        }
    }

    public int getRotation() {
        return display.getRotation();
    }

    @Override
    public void onDisplayAdded(int displayId) {

    }

    @Override
    public void onDisplayRemoved(int displayId) {

    }

    @Override
    public void onDisplayChanged(int displayId) {

    }
}
