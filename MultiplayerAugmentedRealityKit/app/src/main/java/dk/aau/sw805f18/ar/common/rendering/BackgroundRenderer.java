package dk.aau.sw805f18.ar.common.rendering;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.google.ar.core.Frame;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class BackgroundRenderer {
    private static final String TAG = BackgroundRenderer.class.getSimpleName();

    // Shader names
    private static final String VERTEX_SHADER_NAME = "shaders/screenquad.vert";
    private static final String FRAGMENT_SHADER_NAME = "shaders/screenquad.frag";

    private static final int COORDS_PER_VERTEX = 3;
    private static final int TEXCOORDS_PER_VERTEX = 2;
    private static final int FLOAT_SIZE = 4;

    private FloatBuffer mQuadVertices;
    private FloatBuffer mQuadTexCoord;
    private FloatBuffer mQuadTexCoordTransformed;

    private int mQuadProgram;

    private int mQuadPositionParam;
    private int mQuadTexCoordParam;
    private int mTextureId = -1;

    public BackgroundRenderer() {
    }

    public int getTextureId() {
        return mTextureId;
    }

    public void createOnGlThread(Context context) throws IOException {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        mTextureId = textures[0];
        int textureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
        GLES20.glBindTexture(textureTarget, mTextureId);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        int numVertices = 4;
        if (numVertices != QUAD_COORDS.length / COORDS_PER_VERTEX) {
            throw new RuntimeException("Unexpected number of vertices in BackgroundRenderer.");
        }

        ByteBuffer bbVertices = ByteBuffer.allocateDirect(QUAD_COORDS.length * FLOAT_SIZE);
        bbVertices.order(ByteOrder.nativeOrder());

        mQuadVertices = bbVertices.asFloatBuffer();
        mQuadVertices.put(QUAD_COORDS);
        mQuadVertices.position(0);

        ByteBuffer bbTexCoords = ByteBuffer.allocateDirect(numVertices * TEXCOORDS_PER_VERTEX * FLOAT_SIZE);
        bbTexCoords.order(ByteOrder.nativeOrder());
        mQuadTexCoord = bbTexCoords.asFloatBuffer();
        mQuadTexCoord.put(QUAD_TEXCOORDS);
        mQuadTexCoord.position(0);

        ByteBuffer bbTexCoordsTransformed = ByteBuffer.allocateDirect(numVertices * TEXCOORDS_PER_VERTEX * FLOAT_SIZE);
        bbTexCoordsTransformed.order(ByteOrder.nativeOrder());
        mQuadTexCoordTransformed = bbTexCoordsTransformed.asFloatBuffer();

        int vertexShader = ShaderUtil.loadGlShader(TAG, context, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_NAME);
        int fragmentShader = ShaderUtil.loadGlShader(TAG, context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME);

        mQuadProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mQuadProgram, vertexShader);
        GLES20.glAttachShader(mQuadProgram, fragmentShader);
        GLES20.glLinkProgram(mQuadProgram);
        GLES20.glUseProgram(mQuadProgram);

        ShaderUtil.checkGlError(TAG, "Program creation");

        mQuadPositionParam = GLES20.glGetAttribLocation(mQuadProgram, "a_Position");
        mQuadTexCoordParam = GLES20.glGetAttribLocation(mQuadProgram, "a_TexCoord");

        ShaderUtil.checkGlError(TAG, "Program parameters");
    }

    public void draw(Frame frame) {
        // if display rotation changed, we need to re-query the uv coordinates
        if (frame.hasDisplayGeometryChanged()) {
            frame.transformDisplayUvCoords(mQuadTexCoord, mQuadTexCoordTransformed);
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthMask(false);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureId);

        GLES20.glUseProgram(mQuadProgram);

        // Set the vertex positions.
        GLES20.glVertexAttribPointer(
                mQuadPositionParam,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                0,
                mQuadVertices
        );

        // Set the texture coordinates.
        GLES20.glVertexAttribPointer(
                mQuadTexCoordParam,
                TEXCOORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                0,
                mQuadTexCoordTransformed
        );

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(mQuadPositionParam);
        GLES20.glEnableVertexAttribArray(mQuadTexCoordParam);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(mQuadPositionParam);
        GLES20.glDisableVertexAttribArray(mQuadTexCoordParam);

        // Restore the depth state for further drawing.
        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        ShaderUtil.checkGlError(TAG, "Draw");
    }

    private static final float[] QUAD_COORDS = new float[]{
            -1.0f, -1.0f, 0.0f, -1.0f, +1.0f, 0.0f, +1.0f, -1.0f, 0.0f, +1.0f, +1.0f, 0.0f,
    };

    private static final float[] QUAD_TEXCOORDS = new float[]{
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
    };
}
