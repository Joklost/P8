package dk.aau.sw805f18.multiplayeraugmentedrealitykit.common.helpers.gestures;

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
