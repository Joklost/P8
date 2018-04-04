package dk.aau.sw805f18.ar.common.helpers.gestures;

import android.view.MotionEvent;

public abstract class GestureEvent {
    private final MotionEvent motion;

    GestureEvent(MotionEvent e) {
        motion = e;
    }

    public MotionEvent getMotion() {
        return motion;
    }
}
