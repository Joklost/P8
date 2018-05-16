package dk.aau.sw805f18.ar.argame;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import dk.aau.sw805f18.ar.R;

public class ArGameActivity extends AppCompatActivity {
    private static final String TAG = ArGameActivity.class.getSimpleName();

    private ArFragment mArFragment;
    private ModelRenderable mAndyRenderable;

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar_ux);
        mArFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        ModelRenderable.builder()
                .setSource(this, R.raw.treasure)
                .build()
                .thenAccept(modelRenderable -> mAndyRenderable = modelRenderable)
                .exceptionally(throwable -> {
                    Toast toast = Toast.makeText(this, "Unable to load any renderable", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    return null;
                });

        mArFragment.setOnTapArPlaneListener(
                (hitResult, plane, motionEvent) -> {
                    if (mAndyRenderable == null) {
                        return;
                    }

                    if (plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING) {
                        return;
                    }

                    // Create the Anchor
                    Anchor anchor = hitResult.createAnchor();
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(mArFragment.getArSceneView().getScene());

                    // Create the transformable Andy, and add it to the anchor.
                    TransformableNode andy = new TransformableNode(mArFragment.getTransformationSystem());
                    andy.setParent(anchorNode);
                    andy.setRenderable(mAndyRenderable);
//                    andy.select();
                }
        );
    }
}
