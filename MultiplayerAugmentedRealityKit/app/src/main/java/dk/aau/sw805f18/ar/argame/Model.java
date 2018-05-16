package dk.aau.sw805f18.ar.argame;

import com.google.ar.sceneform.rendering.ModelRenderable;

public class Model {
    private int mId;
    private String mTitle;
    private ModelRenderable mRenderableModel;

    Model(int id, String title, ModelRenderable modelRenderable) {
        this.mId = id;
        this.mTitle = title;
        this.mRenderableModel = modelRenderable;
    }

    public int getId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }

    public ModelRenderable getRenderableModel() {
        return mRenderableModel;
    }
}
