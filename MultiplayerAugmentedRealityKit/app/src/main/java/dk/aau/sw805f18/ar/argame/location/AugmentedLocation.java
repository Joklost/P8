package dk.aau.sw805f18.ar.argame.location;

import android.location.Location;

import com.google.ar.core.Anchor;

public class AugmentedLocation {

    private final int mId;

    private final String mModel;
    // Location in real-world terms
    private Location mLocation;
    // Location in AR terms
    private Anchor mAnchor;

    private boolean mLocked;

    public AugmentedLocation(int id, Location location, String model) {
        mId = id;
        mLocation = location;
        mModel = model;
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

    public String getModel() {
        return mModel;
    }

    public int getId() {
        return mId;
    }

}
