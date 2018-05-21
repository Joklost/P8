package dk.aau.sw805f18.ar.argame;

import com.google.ar.core.Anchor;

public class AnchorRenderable {
    private int mId;
    private String mModel;
    private Anchor mAnchor;

    AnchorRenderable(int id, String model, Anchor anchor) {
        this.mId = id;
        this.mModel = model;
        this.mAnchor = anchor;
    }

    public int getId() {
        return mId;
    }

    public String getModel() {
        return mModel;
    }

    public Anchor getAnchor() {
        return mAnchor;
    }
}
