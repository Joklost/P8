package dk.aau.sw805f18.ar.models;

import android.location.Location;

public class WifiP2pData {
    private double mLatitude;
    private double mLongitude;
    private short mRotation;
    private short mScale;
    private int mControlPostId;


    //region getters and setters
    public WifiP2pData() {
    }

    public double getLongitude() {
        return mLongitude;
    }

    public void setLongitude(double longitude) {
        this.mLongitude = longitude;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public void setLatitude(double latitude) {
        this.mLatitude = latitude;
    }

    public short getRotation() {
        return mRotation;
    }

    public void setRotation(short rotation) {
        this.mRotation = rotation;
    }

    public short getScale() {
        return mScale;
    }

    public void setScale(short scale) {
        this.mScale = scale;
    }

    public int getControlPostId() {
        return mControlPostId;
    }

    public void setControlPostId(int controlPostId) {
        this.mControlPostId = controlPostId;
    }
    //endregion
}
