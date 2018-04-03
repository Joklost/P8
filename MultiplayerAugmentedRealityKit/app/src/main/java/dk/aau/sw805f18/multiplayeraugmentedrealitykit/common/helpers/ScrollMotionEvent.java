package dk.aau.sw805f18.multiplayeraugmentedrealitykit.common.helpers;

import android.view.MotionEvent;

public class ScrollMotionEvent {
    private final MotionEvent initialDown;
    private final MotionEvent currentMove;
    private final float distanceX;
    private final float distanceY;


    ScrollMotionEvent(MotionEvent initialDown, MotionEvent currentMove, float distanceX, float distanceY) {
        this.initialDown = initialDown;
        this.currentMove = currentMove;
        this.distanceX = distanceX;
        this.distanceY = distanceY;
    }

    public MotionEvent getInitialDown() {
        return initialDown;
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
