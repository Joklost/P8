package dk.aau.sw805f18.ar.ar.location.rendering;

import android.content.Context;

import java.io.IOException;

public interface Renderer {
    void updateModelMatrix(float[] anchorMatrix, float scale, float rotation);
    void draw(float[] viewMatrix, float[] projectionMatrix, float[] colorCorrectionRgba, float lightIntensity);
    void createOnGlThread(Context context, int markerDistance) throws IOException;
}
