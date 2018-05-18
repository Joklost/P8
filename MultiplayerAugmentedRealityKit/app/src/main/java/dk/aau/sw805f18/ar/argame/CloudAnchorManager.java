package dk.aau.sw805f18.ar.argame;

import android.support.annotation.Nullable;
import android.support.v4.util.Preconditions;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;

import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;

public class CloudAnchorManager {
    private static final String TAG = ArGameActivity.class.getSimpleName() + "." + CloudAnchorManager.class.getSimpleName();

    interface CloudAnchorListener {
        void onCloudTaskComplete(Anchor anchor);
    }

    @Nullable private Session mSession = null;
    private final HashMap<Anchor, CloudAnchorListener> mPendingAnchors = new HashMap<>();

    void setSession(Session session) {
        mSession = session;
    }

    void hostCloudAnchor(Anchor anchor, CloudAnchorListener listener) {
        if (mSession == null) {
            Log.e(TAG, "Session was null, please set Session!");
            return;
        }

        Anchor newAnchor = mSession.hostCloudAnchor(anchor);
        mPendingAnchors.put(newAnchor, listener);
    }

    void resolveCloudAnchor(String anchorId, CloudAnchorListener listener) {
        if (mSession == null) {
            Log.e(TAG, "Session was null, please set Session!");
            return;
        }

        Anchor newAnchor = mSession.resolveCloudAnchor(anchorId);
        mPendingAnchors.put(newAnchor, listener);
    }

    void onUpdate(Collection<Anchor> updatedAnchors) {
        if (mSession == null) {
            Log.e(TAG, "Session was null, please set Session!");
            return;
        }

        for (Anchor anchor : updatedAnchors) {
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

    void clearListeners() {
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
