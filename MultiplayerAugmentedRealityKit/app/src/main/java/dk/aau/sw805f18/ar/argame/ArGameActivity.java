package dk.aau.sw805f18.ar.argame;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.GuardedBy;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.gson.Gson;

import java.util.Collection;
import java.util.HashMap;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.argame.location.AugmentedLocation;
import dk.aau.sw805f18.ar.argame.location.AugmentedLocationManager;
import dk.aau.sw805f18.ar.common.helpers.SyncServiceHelper;
import dk.aau.sw805f18.ar.common.sensor.DeviceLocation;
import dk.aau.sw805f18.ar.common.websocket.Packet;
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

    private enum HostResolveMode {
        NONE,
        HOSTING,
        RESOLVING,
    }


    @GuardedBy("hostResolveLock")
    private HostResolveMode mCurrentHostResolveMode;

    private HashMap<Integer, Model> mModels;
    private ArFragment mArFragment;
    private AugmentedLocationManager mAugmentedLocationManager;
    private TreasureHunt mGame;
    private CloudAnchorManager mCloudAnchorManager;
    private HashMap<Integer, Anchor> mAnchors;
    private Gson mGson;

    @SuppressLint("UseSparseArrays")
    private void buildRenderables() {
        mModels = new HashMap<>();
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

    @SuppressLint("UseSparseArrays")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar_ux);
        mArFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        buildRenderables();

        mAnchors = new HashMap<>();
        mCloudAnchorManager = new CloudAnchorManager();
        mCurrentHostResolveMode = HostResolveMode.NONE;
        mAugmentedLocationManager = new AugmentedLocationManager(this);
        mGame = new TreasureHunt(this, mAugmentedLocationManager);
//        mGame.startGame();
        mGson = new Gson();

        if (SyncServiceHelper.getInstance().isHostingWifiP2p()) {
            mCurrentHostResolveMode = HostResolveMode.HOSTING;
        } else {
            mCurrentHostResolveMode = HostResolveMode.RESOLVING;
            SyncServiceHelper.getInstance().getWifiP2pSocket().attachHandler(Packet.ANCHOR_TYPE, packet -> {
                CloudAnchorInfo cloudAnchorInfo = mGson.fromJson(packet.Data, CloudAnchorInfo.class);
                resolveNode(cloudAnchorInfo.CloudAnchorId, cloudAnchorInfo.Id, cloudAnchorInfo.ModelId);
            });
        }

        // region augmentadd
        mAugmentedLocationManager.add(
                new AugmentedLocation(
                        0,
                        DeviceLocation.BuildLocation(57.013973, 9.988686),
                        R.raw.treasure
                )
        );
        mAugmentedLocationManager.add(
                new AugmentedLocation(
                        1,
                        DeviceLocation.BuildLocation(57.013833, 9.988444),
                        R.raw.treasure
                )
        );
        mAugmentedLocationManager.add(
                new AugmentedLocation(
                        2,
                        DeviceLocation.BuildLocation(57.014007, 9.988455),
                        R.raw.treasure
                )
        );
        // endregion

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
                            addNode(anchor, -1, choice);
                        });
                    });

                }
        );

        // The onUpdate method is called before each frame.
        mArFragment.getArSceneView().getScene().setOnUpdateListener(frameTime -> {
            mArFragment.onUpdate(frameTime);
            mAugmentedLocationManager.update(mArFragment.getArSceneView());
//            mGame.update(mArFragment.getArSceneView().getArFrame());

            Collection<Anchor> updatedAnchors = mArFragment.getArSceneView().getArFrame().getUpdatedAnchors();
            mCloudAnchorManager.onUpdate(updatedAnchors);

            if (mCurrentHostResolveMode == HostResolveMode.HOSTING) {
                mAugmentedLocationManager.update(mArFragment.getArSceneView());
            }
        });

        Session session = mArFragment.getArSceneView().getSession();
        Config config = new Config(session);
        config.setCloudAnchorMode(Config.CloudAnchorMode.ENABLED); // Add this line.
        session.configure(config);
        mCloudAnchorManager.setSession(session);
    }

    public float calcPoseDistance(Pose start, Pose end) {
        float dx = start.tx() - end.tx();
        float dy = start.ty() - end.ty();
        float dz = start.tz() - end.tz();

        // Compute the straight-line distance.
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public Anchor addNode(Anchor anchor, int id, int model) {
        if (mCurrentHostResolveMode != HostResolveMode.HOSTING) {
            Log.e(TAG, "We should only be creating an anchor in hosting mode!");
            return null;
        }

        mAnchors.put(id, anchor);
        Anchor newAnchor = mArFragment.getArSceneView().getSession().hostCloudAnchor(anchor);
        AnchorNode anchorNode = new AnchorNode(newAnchor);
        anchorNode.setParent(mArFragment.getArSceneView().getScene());

        mCloudAnchorManager.hostCloudAnchor(newAnchor, cloudAnchor -> {
            Anchor.CloudAnchorState state = cloudAnchor.getCloudAnchorState();
            if (state.isError()) {
                Log.e(TAG, "Error hosting a cloud anchor. State: " + state);
                return;
            }

            SyncServiceHelper.getInstance().getWebSocketeerServer().sendToAll(
                    new Packet(
                            Packet.ANCHOR_TYPE,
                            mGson.toJson(new CloudAnchorInfo(id, cloudAnchor.getCloudAnchorId(), model))
                    )
            );
        });

        addTransformableNode(anchorNode, model);
        return newAnchor;
    }

    private void resolveNode(String cloudAnchorId, int id, int model) {
        if (mCurrentHostResolveMode != HostResolveMode.RESOLVING) {
            Log.e(TAG, "We should only be resolving an anchor in resolving mode!");
            return;
        }

        mCloudAnchorManager.resolveCloudAnchor(cloudAnchorId, anchor -> {
            mAnchors.put(id, anchor);
            AnchorNode anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(mArFragment.getArSceneView().getScene());
            addTransformableNode(anchorNode, model);
        });
    }

    private void addTransformableNode(AnchorNode anchorNode, int model) {
        // Create the transformable model, and attach it to the anchor.
        TransformableNode transformableNode = new TransformableNode(mArFragment.getTransformationSystem());
        // TODO: Disable transformation?
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
