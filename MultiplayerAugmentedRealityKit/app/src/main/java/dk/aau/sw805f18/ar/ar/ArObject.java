package dk.aau.sw805f18.ar.ar;

import com.google.ar.core.Anchor;

public class ArObject {
    private Anchor mAnchor;
    private ArModel mModel;
    private float mRotation;
    private float mScale;

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

    public float getRotation() {
        return mRotation;
    }

    public void setRotation(float rotation) {
        if (rotation < 0.0f && rotation > 360.0f) {
            throw new RuntimeException("mRotation value not within bounds");
        }
        mRotation = rotation;
    }

    public float getScale() {
        return mScale;
    }

    public void setScale(float scale) {
        mScale = scale;
    }
}
