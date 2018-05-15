package dk.aau.sw805f18.ar.common.rendering;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class AnnotationRenderer implements Renderer {
    private static final String TAG = "AnnotationRenderer";

    private int[] mTextures = new int[1];

    private float[] QUAD_COORDS = new float[] {
            // x, y, z
            -.7f, -.7f, 0.0f,
            -.7f, +.7f, 0.0f,
            +.7f, -.7f, 0.0f,
            +.7f, +.7f, 0.0f,
    };

    private static final float[] QUAD_TEXCOORDS = new float[] {
            // x, y
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
    };

    private static final int COORDS_PER_VERTEX = 3;
    private static final int TEXCOORDS_PER_VERTEX = 2;

    private static final String VERTEX_SHADER = "shaders/annotation.vert";
    private static final String FRAGMENT_SHADER = "shaders/annotation.frag";

    private FloatBuffer mQuadVertices;
    private FloatBuffer mQuadTexCoord;
    private int mQuadProgram;
    private int mQuadPositionParam;
    private int mQuadTexCoordParam;
    private int mTextureUniform;
    private int mModelViewProjectionUniform;

    // Temporary matrices allocated here to reduce number of allocations for each frame.
    private float[] mModelMatrix = new float[16];
    private float[] mModelViewMatrix = new float[16];
    private float[] mModelViewProjectionMatrix = new float[16];

    public String getmAnnotationText() {
        return mAnnotationText;
    }

    private String mAnnotationText;
    public AnnotationRenderer(Object... data) {
        mAnnotationText = (String) data[0];
    }

    @Override
    public void createOnGlThread(Context context, int distance) {
        // Read the texture.
        Bitmap textureBitmap;
        try {
            textureBitmap = drawTextToAnnotation(mAnnotationText, metresReadable(distance));
            textureBitmap.setHasAlpha(true);
        } catch (Exception e) {
            Log.e(TAG, "Exception reading texture", e);
            return;
        }

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glGenTextures(mTextures.length, mTextures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        GLES20.glBlendColor(0,0,0,0);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, textureBitmap, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        textureBitmap.recycle();

        ShaderUtil.checkGlError(TAG, "Texture loading");

        // Build the geometry of a simple imageRenderer.

        int numVertices = 4;
        if (numVertices != QUAD_COORDS.length / COORDS_PER_VERTEX) {
            throw new RuntimeException("Unexpected number of vertices in BackgroundRenderer.");
        }

        ByteBuffer bbVertices = ByteBuffer.allocateDirect(QUAD_COORDS.length * Float.BYTES);
        bbVertices.order(ByteOrder.nativeOrder());
        mQuadVertices = bbVertices.asFloatBuffer();
        mQuadVertices.put(QUAD_COORDS);
        mQuadVertices.position(0);

        ByteBuffer bbTexCoords =
                ByteBuffer.allocateDirect(numVertices * TEXCOORDS_PER_VERTEX * Float.BYTES);
        bbTexCoords.order(ByteOrder.nativeOrder());
        mQuadTexCoord = bbTexCoords.asFloatBuffer();
        mQuadTexCoord.put(QUAD_TEXCOORDS);
        mQuadTexCoord.position(0);

        ByteBuffer bbTexCoordsTransformed =
                ByteBuffer.allocateDirect(numVertices * TEXCOORDS_PER_VERTEX * Float.BYTES);
        bbTexCoordsTransformed.order(ByteOrder.nativeOrder());

        int vertexShader = 0;
        int fragmentShader = 0;
        try {
            vertexShader = ShaderUtil.loadGlShader(TAG, context, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
            fragmentShader = ShaderUtil.loadGlShader(TAG, context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        } catch (IOException ignored) {
            return;
        }

        mQuadProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mQuadProgram, vertexShader);
        GLES20.glAttachShader(mQuadProgram, fragmentShader);
        GLES20.glLinkProgram(mQuadProgram);
        GLES20.glUseProgram(mQuadProgram);

        ShaderUtil.checkGlError(TAG, "Program creation");

        mQuadPositionParam = GLES20.glGetAttribLocation(mQuadProgram, "a_Position");
        mQuadTexCoordParam = GLES20.glGetAttribLocation(mQuadProgram, "a_TexCoord");
        mTextureUniform = GLES20.glGetUniformLocation(mQuadProgram, "u_Texture");
        mModelViewProjectionUniform =
                GLES20.glGetUniformLocation(mQuadProgram, "u_ModelViewProjection");

        ShaderUtil.checkGlError(TAG, "Program parameters");

        Matrix.setIdentityM(mModelMatrix, 0);
    }

    private String metresReadable(int metres) {
        if(metres < 1000)
            return Integer.toString(metres) + "M";
        else
            return Integer.toString(Math.round(metres / 1000)) + "KM";
    }

    private Bitmap drawTextToAnnotation(String gText, String gDistance) {

        float shadow_size = 16f;
        int font_size = 120;
        int distance_font_size = 90;
        int line_stroke = 15;
        int vertical_line_length = 300;
        int annotation_color = Color.rgb(225, 225, 225);
        int shadow_color = Color.rgb(190,190,190);


        int total_text_width = 0;
        Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        Bitmap bitmap_object = Bitmap.createBitmap(800, 800, conf);
        bitmap_object.setHasAlpha(true);

        Canvas canvas = new Canvas(bitmap_object);

        // DRAWING LABEL
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
        paint.setAntiAlias(true);
        paint.setColor(annotation_color);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setTextSize(font_size);
        paint.setShadowLayer(shadow_size, 0f, 3f, shadow_color);
        Rect bounds = new Rect();
        paint.getTextBounds(gText, 0, gText.length(), bounds);
        total_text_width += bounds.width();
        int x = (int) shadow_size;
        int y = font_size + (int) shadow_size;
        canvas.drawText(gText, x, y, paint);

        // DRAW DISTANCE (100M/4.5KM)
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(annotation_color);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setTextSize(distance_font_size);
        paint.setShadowLayer(shadow_size, 0f, 3f, Color.rgb(150,150,150));
        bounds = new Rect();
        paint.getTextBounds(gDistance, 0, gDistance.length(), bounds);
        x = (int) shadow_size;
        y = font_size + (int) shadow_size + distance_font_size + 20;
        canvas.drawText(gDistance, x, y, paint);

        // DRAW LINES

        // horizontal
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(annotation_color);
        paint.setStrokeWidth(0);
        canvas.drawRect(Math.round(line_stroke/2), y + 35, total_text_width + 100, y + 35 + line_stroke, paint);

        // vertical
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(annotation_color);
        paint.setStrokeWidth(0);
        canvas.drawRect(Math.round(line_stroke/2), y + 35, Math.round(line_stroke/2) + line_stroke, y + 35 + vertical_line_length, paint);

        // DRAW SMALL CIRCLE
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(225, 225, 225));
        paint.setStrokeWidth(0);
        canvas.drawCircle(line_stroke,y + 35 + vertical_line_length - line_stroke, line_stroke, paint);

        // Translate canvas to keep annotation central
        canvas.translate( (total_text_width + 100) /2f,0);


        return bitmap_object;
    }
//
//    private int loadGLShader(String tag, int type, String source) {
//        int shader = GLES20.glCreateShader(type);
//        GLES20.glShaderSource(shader, source);
//        GLES20.glCompileShader(shader);
//
//        // Get the compilation status.
//        final int[] compileStatus = new int[1];
//        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
//
//        // If the compilation failed, delete the shader.
//        if (compileStatus[0] == 0) {
//            Log.e(tag, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
//            GLES20.glDeleteShader(shader);
//            shader = 0;
//        }
//
//        if (shader == 0) {
//            throw new RuntimeException("Error creating shader.");
//        }
//
//        return shader;
//    }

    @Override
    public void updateModelMatrix(float[] modelMatrix, float scaleFactor, float rotation) {
        float[] scaleMatrix = new float[16];
        Matrix.setIdentityM(scaleMatrix, 0);
        scaleMatrix[0] = scaleFactor;
        scaleMatrix[5] = scaleFactor;
        scaleMatrix[10] = scaleFactor;
        Matrix.multiplyMM(mModelMatrix, 0, modelMatrix, 0, scaleMatrix, 0);
    }

    @Override
    public void draw(float[] cameraView, float[] cameraPerspective, float[] colorCorrectionRgba, float lightIntensity) {
        ShaderUtil.checkGlError(TAG, "Before draw");
        Matrix.multiplyMM(mModelViewMatrix, 0, cameraView, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mModelViewProjectionMatrix, 0, cameraPerspective, 0, mModelViewMatrix, 0);

        GLES20.glUseProgram(mQuadProgram);

        // Attach the object texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glBlendColor(0,0,0,0);


        GLES20.glUniform1i(mTextureUniform, 0);
        GLES20.glUniformMatrix4fv(mModelViewProjectionUniform, 1, false, mModelViewProjectionMatrix, 0);
        // Set the vertex positions.
        GLES20.glVertexAttribPointer(
                mQuadPositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, mQuadVertices);

        // Set the texture coordinates.
        GLES20.glVertexAttribPointer(
                mQuadTexCoordParam, TEXCOORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, mQuadTexCoord);

        // Enable vertex arrays
        GLES20.glEnableVertexAttribArray(mQuadPositionParam);
        GLES20.glEnableVertexAttribArray(mQuadTexCoordParam);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(mQuadPositionParam);
        GLES20.glDisableVertexAttribArray(mQuadTexCoordParam);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

        ShaderUtil.checkGlError(TAG, "After draw");
    }
}