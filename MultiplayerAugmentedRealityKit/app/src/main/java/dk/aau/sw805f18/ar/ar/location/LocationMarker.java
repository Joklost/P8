package dk.aau.sw805f18.ar.ar.location;

import android.location.Location;

import com.google.ar.core.Anchor;

import dk.aau.sw805f18.ar.common.rendering.Renderer;

public class LocationMarker {

    // Location in real-world terms
    private Location mLocation;
    // Location in AR terms
    private Anchor mAnchor;

    private boolean mLocked;

    //Renderer
    private Renderer mRenderer;

    public LocationMarker(Location location, Renderer renderer) {
        this.mLocation = location;
        this.mRenderer = renderer;
    }

    public void setOnTouchListener(Runnable touchEvent) {
        this.touchEvent = touchEvent;
    }

    public Runnable getTouchEvent() {
        return touchEvent;
    }
    private Runnable touchEvent;
    private int touchableSize;

    public int getTouchableSize() {
        return touchableSize;
    }

    public void setTouchableSize(int touchableSize) {
        this.touchableSize = touchableSize;
    }

    public Anchor getAnchor() {
        return mAnchor;
    }

    public void setAnchor(Anchor anchor) {
        this.mAnchor = anchor;
    }

    public Location getLocation() {
        return mLocation;
    }

    public boolean isLocked() {
        return mLocked;
    }

    public void setLocked(boolean locked) {
        this.mLocked = locked;
    }

    public Renderer getRenderer() {
        return mRenderer;
    }
}
