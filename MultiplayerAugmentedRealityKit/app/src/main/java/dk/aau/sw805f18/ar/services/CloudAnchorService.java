package dk.aau.sw805f18.ar.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Session;

import java.util.Collection;
import java.util.HashMap;

import dk.aau.sw805f18.ar.argame.ArGameActivity;
import dk.aau.sw805f18.ar.common.helpers.Task;

public class CloudAnchorService extends Service {
    private static final String TAG = ArGameActivity.class.getSimpleName() + "." + CloudAnchorService.class.getSimpleName();

    private final IBinder mBinder = new LocalBinder();
    @Nullable
    private Session mSession = null;
    private final HashMap<Anchor, CloudAnchorListener> mPendingAnchors = new HashMap<>();

    public interface CloudAnchorListener {
        void onCloudTaskComplete(Anchor anchor);
    }

    public void setSession(Session session) {
        mSession = session;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public CloudAnchorService getService() {
            return CloudAnchorService.this;
        }
    }

    public void hostCloudAnchor(Anchor anchor, CloudAnchorListener listener) {
        if (mSession == null) {
            Log.e(TAG, "Session was null, please set Session!");
            return;
        }

        Anchor newAnchor = mSession.hostCloudAnchor(anchor);
        mPendingAnchors.put(newAnchor, listener);
    }

    public void resolveCloudAnchor(String anchorId, CloudAnchorListener listener) {
        if (mSession == null) {
            Log.e(TAG, "Session was null, please set Session!");
            return;
        }

        Anchor newAnchor = mSession.resolveCloudAnchor(anchorId);
        mPendingAnchors.put(newAnchor, listener);
    }

    public void onUpdate(Collection<Anchor> updatedAnchors) {
        if (mSession == null) {
            Log.e(TAG, "Session was null, please set Session!");
            return;
        }

        Task.run(new UpdateTask(updatedAnchors));
    }

    private class UpdateTask implements Runnable {
        private Collection<Anchor> mUpdatedAnchors;

        UpdateTask(Collection<Anchor> updatedAnchors) {
            this.mUpdatedAnchors = updatedAnchors;
        }

        @Override
        public void run() {
            synchronized (UpdateTask.class) {
                for (Anchor anchor : mUpdatedAnchors) {
                    if (!mPendingAnchors.containsKey(anchor)) {
                        continue;
                    }

                    Anchor.CloudAnchorState cloudState = anchor.getCloudAnchorState();
                    if (isReturnableState(cloudState)) {
                        CloudAnchorListener listener = mPendingAnchors.remove(anchor);
                        listener.onCloudTaskComplete(anchor);
                    }
                }
            }
        }
    }

    private void clearListeners() {
        mPendingAnchors.clear();
    }

    private static boolean isReturnableState(Anchor.CloudAnchorState cloudState) {
        switch (cloudState) {
            case NONE:
            case TASK_IN_PROGRESS:
                return false;
            default:
                return true;
        }
    }
}
