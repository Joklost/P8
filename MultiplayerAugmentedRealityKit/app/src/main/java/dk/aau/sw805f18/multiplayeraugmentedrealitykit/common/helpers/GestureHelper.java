package dk.aau.sw805f18.multiplayeraugmentedrealitykit.common.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class GestureHelper implements View.OnTouchListener {
    private final GestureDetector gestureDetector;
    private final BlockingQueue<ScrollMotionEvent> queuedScrolls = new ArrayBlockingQueue<>(16);

    private final BlockingQueue<MotionEvent> queuedSingleTaps = new ArrayBlockingQueue<>(16);

    public GestureHelper(Context context) {
        gestureDetector = new GestureDetector(context, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                queuedSingleTaps.offer(e);
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                queuedScrolls.offer(new ScrollMotionEvent(e1, e2, distanceX, distanceY));
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return false;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }
        });
    }

    public MotionEvent pollTaps() {
        return queuedSingleTaps.poll();
    }

    public ScrollMotionEvent pollScrolls() {
        return queuedScrolls.poll();
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

}
