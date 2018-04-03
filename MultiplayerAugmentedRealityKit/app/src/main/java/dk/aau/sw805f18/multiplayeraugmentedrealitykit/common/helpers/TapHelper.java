package dk.aau.sw805f18.multiplayeraugmentedrealitykit.common.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TapHelper implements View.OnTouchListener {
    private final GestureDetector gestureDetector;
    private final BlockingQueue<MotionEvent> queuedSingleTaps = new ArrayBlockingQueue<>(16);

    public TapHelper(Context context) {
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                queuedSingleTaps.offer(e);
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });
    }

    public MotionEvent poll() {
        return queuedSingleTaps.poll();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

}
