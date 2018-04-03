package dk.aau.sw805f18.multiplayeraugmentedrealitykit.common.rendering;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ShaderUtil {
    public static int loadGlShader(String tag, Context context, int type, String filename)
            throws IOException {
        String code = readRawTextFileFromAssets(context, filename);

        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        // get the compilation status
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        // if the compilation failed, delete the shader
        if (compileStatus[0] == 0) {
            Log.e(tag, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }

        if (shader == 0) {
            throw new RuntimeException("Error creating shader.");
        }

        return shader;
    }

    public static void checkGlError(String tag, String label) {
        int lastError = GLES20.GL_NO_ERROR;

        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(tag, label + ". glError " + error);
            lastError = error;
        }
        if (lastError != GLES20.GL_NO_ERROR) {
            throw new RuntimeException(label + ": glError " + lastError);
        }
    }

    private static String readRawTextFileFromAssets(Context context, String filename)
            throws IOException {
        try (InputStream inputStream = context.getAssets().open(filename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }
}
