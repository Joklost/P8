package dk.aau.sw805f18.ar.ar;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;

public class ArObject {
    private Anchor mAnchor;
    private ArModel mModel;
    private int mRotationDegree = -1;
    private float mScale = 1.0f;

    ArObject(Anchor anchor, ArModel model) {
        mAnchor = anchor;
        mModel = model;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ArObject && mAnchor.equals(((ArObject) obj).mAnchor);
    }

    @Override
    public int hashCode() {
        return mAnchor.hashCode();
    }

    public Anchor getAnchor() {
        return mAnchor;
    }

    public void setAnchor(Anchor anchor) {
        mAnchor = anchor;
    }

    public ArModel getModel() {
        return mModel;
    }

    public void setModel(ArModel model) {
        mModel = model;
    }

    public float getScale() {
        return mScale;
    }

    public void setScale(float scale) {
        mScale = scale;
    }

    public int getRotationDegree() {
        return mRotationDegree;
    }

    public void setRotationDegree(int rotationDegree) {
        mRotationDegree = rotationDegree;
    }

}
