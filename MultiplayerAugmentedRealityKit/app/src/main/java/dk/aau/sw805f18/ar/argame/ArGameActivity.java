package dk.aau.sw805f18.ar.argame;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Plane;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.HashMap;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.argame.location.AugmentedLocation;
import dk.aau.sw805f18.ar.argame.location.AugmentedLocationManager;
import dk.aau.sw805f18.ar.common.sensor.DeviceLocation;
import dk.aau.sw805f18.ar.common.sensor.DeviceOrientation;
import dk.aau.sw805f18.ar.fragments.ModelDialogFragment;

public class ArGameActivity extends AppCompatActivity {
    private static final String TAG = ArGameActivity.class.getSimpleName();

    @SuppressLint("UseSparseArrays")
    private static final HashMap<Integer, String> MODELS = new HashMap<>();

    static {
        MODELS.put(R.raw.andy, "Andy");
        MODELS.put(R.raw.chest, "Chest");
        MODELS.put(R.raw.treasure, "Treasure");
    }

    @SuppressLint("UseSparseArrays")
    private HashMap<Integer, Model> mModels = new HashMap<>();
    private ArFragment mArFragment;
    private AugmentedLocationManager mAugmentedLocationManager;

    private void buildRenderables() {
        for (int model : MODELS.keySet()) {
            // When you build a Renderable, Sceneform loads its resources in the background while returning
            // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
            ModelRenderable.builder()
                    .setSource(this, model)
                    .build()
                    .thenAccept(modelRenderable -> mModels.put(model, new Model(model, MODELS.get(model), modelRenderable)))
                    .exceptionally(throwable -> {
                        Toast toast = Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                        return null;
                    });
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar_ux);
        mArFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        buildRenderables();

        mAugmentedLocationManager = new AugmentedLocationManager(this);
        mAugmentedLocationManager.add(
                new AugmentedLocation(
                        DeviceLocation.BuildLocation(57.014751, 9.978139),
                        R.raw.treasure
                )
        );

        mArFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
                    if (mModels == null || mModels.size() == 0) {
                        return;
                    }

                    if (plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING) {
                        return;
                    }

                    ModelDialogFragment modelDialogFragment = new ModelDialogFragment();
                    runOnUiThread(() -> {
                        modelDialogFragment.show(getSupportFragmentManager(), TAG, mModels, choice -> {
                            if (mModels.get(choice).getRenderableModel() == null) {
                                return;
                            }

                            // Create the Anchor.
                            Anchor anchor = hitResult.createAnchor();
                            addNode(anchor, choice);
                        });
                    });

                }
        );

        // The onUpdate method is called before each frame.
        mArFragment.getArSceneView().getScene().setOnUpdateListener(frameTime -> {
            mArFragment.onUpdate(frameTime);
            mAugmentedLocationManager.update(mArFragment.getArSceneView());
        });
    }

    public void addNode(Anchor anchor, int model) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(mArFragment.getArSceneView().getScene());

        // Create the transformable model, and attach it to the anchor.
        TransformableNode transformableNode = new TransformableNode(mArFragment.getTransformationSystem());
        transformableNode.setParent(anchorNode);
        transformableNode.setRenderable(mModels.get(model).getRenderableModel());
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAugmentedLocationManager.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAugmentedLocationManager.resume();
    }
}
