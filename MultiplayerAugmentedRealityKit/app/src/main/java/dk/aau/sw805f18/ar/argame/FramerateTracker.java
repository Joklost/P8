package dk.aau.sw805f18.ar.argame;

import com.google.ar.sceneform.FrameTime;

import java.util.Queue;

public class FramerateTracker {
    private static final float SMOOTHING = 0.9f;

    private float mMeasurement;

    public float onUpdate(FrameTime frameTime) {
        mMeasurement = (float) ((mMeasurement * SMOOTHING) + (frameTime.getStartSeconds() * 1.0 - SMOOTHING));
        return mMeasurement;
    }
}
