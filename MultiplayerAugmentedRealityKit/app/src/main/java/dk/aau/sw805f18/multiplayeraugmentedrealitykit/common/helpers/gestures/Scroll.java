package dk.aau.sw805f18.multiplayeraugmentedrealitykit.common.helpers.gestures;

import android.view.MotionEvent;

public class Scroll extends GestureEvent {
    private final MotionEvent currentMove;
    private final float distanceX;
    private final float distanceY;


    public Scroll(MotionEvent initialDown, MotionEvent currentMove, float distanceX, float distanceY) {
        super(initialDown);
        this.currentMove = currentMove;
        this.distanceX = distanceX;
        this.distanceY = distanceY;
    }

    public MotionEvent getCurrentMove() {
        return currentMove;
    }

    public float getDistanceX() {
        return distanceX;
    }

    public float getDistanceY() {
        return distanceY;
    }
}
