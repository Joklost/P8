package dk.aau.sw805f18.multiplayeraugmentedrealitykit.common.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import dk.aau.sw805f18.multiplayeraugmentedrealitykit.common.helpers.gestures.Down;
import dk.aau.sw805f18.multiplayeraugmentedrealitykit.common.helpers.gestures.Fling;
import dk.aau.sw805f18.multiplayeraugmentedrealitykit.common.helpers.gestures.GestureEvent;
import dk.aau.sw805f18.multiplayeraugmentedrealitykit.common.helpers.gestures.LongPress;
import dk.aau.sw805f18.multiplayeraugmentedrealitykit.common.helpers.gestures.Scroll;
import dk.aau.sw805f18.multiplayeraugmentedrealitykit.common.helpers.gestures.ShowPress;
import dk.aau.sw805f18.multiplayeraugmentedrealitykit.common.helpers.gestures.Tap;

public class GestureHelper implements View.OnTouchListener {
    private final GestureDetector gestureDetector;
    private final BlockingQueue<GestureEvent> gestureQueue = new ArrayBlockingQueue<>(16);

    private final BlockingQueue<Scroll> queuedScrolls = new ArrayBlockingQueue<>(16);

    private final BlockingQueue<MotionEvent> queuedSingleTaps = new ArrayBlockingQueue<>(16);

    public GestureHelper(Context context) {
        gestureDetector = new GestureDetector(context, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                gestureQueue.offer(new Tap(e));
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                gestureQueue.offer(new Scroll(e1, e2, distanceX, distanceY));
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                gestureQueue.offer(new LongPress(e));
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                gestureQueue.offer(new Fling(e1, e2, velocityX, velocityY));
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                gestureQueue.offer(new Down(e));
                return true;
            }

            @Override
            public void onShowPress(MotionEvent e) {
                gestureQueue.offer(new ShowPress(e));
            }
        });
    }

    public GestureEvent poll() {
        return gestureQueue.poll();
    }

    public MotionEvent pollTaps() {
        return queuedSingleTaps.poll();
    }

    public Scroll pollScrolls() {
        return queuedScrolls.poll();
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

}
