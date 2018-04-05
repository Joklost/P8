package dk.aau.sw805f18.ar.common.helpers.gestures;

import android.view.MotionEvent;

public class Fling extends GestureEvent {
    private final MotionEvent motionUp;
    private final float velocityX;
    private final float velocityY;

    public Fling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        super(e1);
        motionUp = e2;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
    }

    public MotionEvent getMotionUp() {
        return motionUp;
    }

    public float getVelocityX() {
        return velocityX;
    }

    public float getVelocityY() {
        return velocityY;
    }
}
