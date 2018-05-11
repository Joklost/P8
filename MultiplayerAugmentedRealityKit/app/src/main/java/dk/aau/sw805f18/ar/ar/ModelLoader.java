package dk.aau.sw805f18.ar.ar;

import android.content.Context;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import dk.aau.sw805f18.ar.common.rendering.ObjectRenderer;

public class ModelLoader {
    private static final Map<String, ArModel> mAssets = new HashMap<>();

    public static ArModel load(Context context, String assetName) throws IOException {
        if (mAssets.containsKey(assetName)) {
            return mAssets.get(assetName);
        }

        if (context == null) {
            return null;
        }

        ObjectRenderer obj = new ObjectRenderer(
                String.format("models/%s.obj", assetName),
                String.format("models/%s.png", assetName)
        );
        ObjectRenderer objShadow = new ObjectRenderer(
                String.format("models/%s_shadow.obj", assetName),
                String.format("models/%s_shadow.png", assetName)
        );

        obj.createOnGlThread(
                context,
                0
        );
        obj.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

        objShadow.createOnGlThread(
                context,
                0
        );
        objShadow.setBlendMode(ObjectRenderer.BlendMode.Shadow);
        objShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f);

        ArModel asset = new ArModel(obj, objShadow);
        mAssets.put(assetName, asset);
        return asset;
    }
}
