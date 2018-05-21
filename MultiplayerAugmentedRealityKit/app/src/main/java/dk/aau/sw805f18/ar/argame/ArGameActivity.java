package dk.aau.sw805f18.ar.argame;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.GuardedBy;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.argame.location.AugmentedLocation;
import dk.aau.sw805f18.ar.argame.location.AugmentedLocationManager;
import dk.aau.sw805f18.ar.common.helpers.CloudAnchorServiceHelper;
import dk.aau.sw805f18.ar.common.helpers.SyncServiceHelper;
import dk.aau.sw805f18.ar.common.helpers.Task;
import dk.aau.sw805f18.ar.common.sensor.DeviceLocation;
import dk.aau.sw805f18.ar.common.websocket.Packet;
import dk.aau.sw805f18.ar.fragments.ModelDialogFragment;
import dk.aau.sw805f18.ar.services.CloudAnchorService;
import dk.aau.sw805f18.ar.services.SyncService;

public class ArGameActivity extends AppCompatActivity {
    private static final String TAG = ArGameActivity.class.getSimpleName();

    @SuppressLint("UseSparseArrays")
    private static final HashMap<String, Integer> MODELS = new HashMap<>();

    static {
        MODELS.put("Andy", R.raw.andy);
        MODELS.put("Chest", R.raw.chest);
        MODELS.put("Treasure", R.raw.treasure);
    }

    private enum HostResolveMode {
        NONE,
        HOSTING,
        RESOLVING,
    }


    @GuardedBy("hostResolveLock")
    private HostResolveMode mCurrentHostResolveMode;

    private HashMap<String, Model> mModels;
    private ArFragment mArFragment;
    private AugmentedLocationManager mAugmentedLocationManager;
    private TreasureHunt mGame;

    public HashMap<Anchor, Integer> getAnchors() {
        return mAnchors;
    }

    public HashMap<Integer, Anchor> getAnchorsReverse() {
        return mAnchorsReverse;
    }

    private HashMap<Integer, Anchor> mAnchorsReverse;
    private HashMap<Anchor, Integer> mAnchors;

    private Gson mGson;

    private ArrayBlockingQueue<AnchorRenderable> mAnchorQueue;


    private CloudAnchorService mCloudAnchorService;
    private SyncService mSyncService;

    private TextView mDebugText;

    private ViewRenderable mTextViewRenderable;

    @SuppressLint("UseSparseArrays")
    private void buildRenderables() {
        mModels = new HashMap<>();
        for (Map.Entry<String, Integer> entry : MODELS.entrySet()) {
            // When you build a Renderable, Sceneform loads its resources in the background while returning
            // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
            ModelRenderable.builder()
                    .setSource(this, entry.getValue())
                    .build()
                    .thenAccept(modelRenderable -> mModels.put(entry.getKey(), new Model(entry.getValue(), entry.getKey(), modelRenderable)))
                    .exceptionally(throwable -> {
                        Toast toast = Toast.makeText(this, "Unable to load renderable", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                        return null;
                    });
        }

        ViewRenderable.builder()
                .setView(this, R.layout.id_view)
                .build()
                .thenAccept(viewRenderable -> mTextViewRenderable = viewRenderable);
    }

    @SuppressLint("UseSparseArrays")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar_ux);
        mArFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        buildRenderables();

        mGson = new Gson();
        mAnchors = new HashMap<>();
        mAnchorsReverse = new HashMap<>();
        mAnchorQueue = new ArrayBlockingQueue<AnchorRenderable>(64);
        mAugmentedLocationManager = new AugmentedLocationManager(this);
        mCurrentHostResolveMode = HostResolveMode.NONE;
        mSyncService = SyncServiceHelper.getInstance();

        mDebugText = findViewById(R.id.debugText);

        mGame = new TreasureHunt(this, mAugmentedLocationManager);


        // region augmentadd
//        mAugmentedLocationManager.add(
////                new AugmentedLocation(
////                        0,
////                        DeviceLocation.BuildLocation(57.013973, 9.988686),
////                        R.raw.treasure
////                )
////        );
////        mAugmentedLocationManager.add(
////                new AugmentedLocation(
////                        1,
////                        DeviceLocation.BuildLocation(57.013833, 9.988444),
////                        R.raw.treasure
////                )
////        );
////        mAugmentedLocationManager.add(
////                new AugmentedLocation(
////                        2,
////                        DeviceLocation.BuildLocation(57.014007, 9.988455),
////                        R.raw.treasure
////                )
////        );
//
//
        mAugmentedLocationManager.add(
                new AugmentedLocation(
                        0,
                        DeviceLocation.BuildLocation(57.014737, 9.978055),
                        "Treasure"
                )
        );
        mAugmentedLocationManager.add(
                new AugmentedLocation(
                        1,
                        DeviceLocation.BuildLocation(57.014644, 9.978362),
                        "Treasure"
                )
        );
        mAugmentedLocationManager.add(
                new AugmentedLocation(
                        2,
                        DeviceLocation.BuildLocation(57.014477, 9.978343),
                        "Treasure"
                )
        );
        // endregion

        if (mSyncService.IsOwner()) {
            mCurrentHostResolveMode = HostResolveMode.HOSTING;
            runOnUiThread(() -> {
                String s = mDebugText.getText() + "\n"
                        + getResources().getText(R.string.is_owner).toString();
                mDebugText.setText(s);
            });
        } else {
            if (mSyncService.getWebSocket() == null) {
                mCurrentHostResolveMode = HostResolveMode.HOSTING;
            } else {
                mCurrentHostResolveMode = HostResolveMode.RESOLVING;
                mSyncService.getWebSocket().attachHandler(Packet.ANCHOR_TYPE, packet -> {
                    CloudAnchorInfo cloudAnchorInfo = mGson.fromJson(packet.Data, CloudAnchorInfo.class);
                    resolveNode(cloudAnchorInfo.CloudAnchorId, cloudAnchorInfo.Id, cloudAnchorInfo.Model);
                    Log.i(TAG, "received anchor");
                });
            }
        }

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
                            addNode(anchor, 1, choice);
                        });
                    });

                });

        // The onUpdate method is called before each frame.
        mArFragment.getArSceneView().getScene().setOnUpdateListener(frameTime -> {
            mArFragment.onUpdate(frameTime);
            mGame.update(mArFragment.getArSceneView().getArFrame());

            if (mCloudAnchorService != null) {
                Collection<Anchor> updatedAnchors = mArFragment.getArSceneView().getArFrame().getUpdatedAnchors();
                mCloudAnchorService.onUpdate(updatedAnchors);
            }

            if (mCurrentHostResolveMode == HostResolveMode.HOSTING && mArFragment.getArSceneView().getSession() != null) {
                mAugmentedLocationManager.onUpdate(mArFragment.getArSceneView());
            }

            AnchorRenderable anchorRenderable = mAnchorQueue.poll();
            if (anchorRenderable != null) {
                mAnchors.put(anchorRenderable.mAnchor, anchorRenderable.getId());
                addTransformableNode(anchorRenderable.getAnchor(), anchorRenderable.getId(), anchorRenderable.mModel);
            }
        });
    }

    public float calcPoseDistance(Pose start, Pose end) {
        float dx = start.tx() - end.tx();
        float dy = start.ty() - end.ty();
        float dz = start.tz() - end.tz();

        // Compute the straight-line distance.
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public Anchor addNode(Anchor anchor, int id, String model) {

        mAnchors.put(anchor, id);
        mAnchorsReverse.put(id, anchor);

        Anchor newAnchor = mArFragment.getArSceneView().getSession().hostCloudAnchor(anchor);
        addTransformableNode(newAnchor, id, model);

        if (mCurrentHostResolveMode != HostResolveMode.HOSTING) {
            Log.e(TAG, "We should only be creating an anchor in hosting mode!");
            return null;
        }

        runOnUiThread(() -> {
            String s = mDebugText.getText() + "\n" + id + "\t"
                    + getResources().getText(R.string.hosting_anchor).toString();
            mDebugText.setText(s);
        });

        mCloudAnchorService.hostCloudAnchor(newAnchor, cloudAnchor -> {
            Anchor.CloudAnchorState state = cloudAnchor.getCloudAnchorState();
            if (state.isError()) {
                Log.e(TAG, "Error hosting a cloud anchor. State: " + state);
                return;
            }

            mSyncService.getWebSocket().send(
                    new Packet(
                            Packet.ANCHOR_TYPE,
                            mGson.toJson(new CloudAnchorInfo(id, cloudAnchor.getCloudAnchorId(), model))
                    )

            );

            runOnUiThread(() -> {
                String s = mDebugText.getText() + "\n" + id + "\t"
                        + getResources().getText(R.string.anchor_hosted).toString();
                mDebugText.setText(s);
            });

            Log.i(TAG, "send new anchor");

        });

        return newAnchor;
    }


    private class AnchorRenderable {
        private int mId;
        private String mModel;
        private Anchor mAnchor;

        public AnchorRenderable(int id, String model, Anchor anchor) {
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

    private void resolveNode(String cloudAnchorId, int id, String model) {
        if (mCurrentHostResolveMode != HostResolveMode.RESOLVING) {
            Log.e(TAG, "We should only be resolving an anchor in resolving mode!");
            return;
        }

        runOnUiThread(() -> {
            String s = mDebugText.getText() + "\n" + id + "\t"
                    + getResources().getText(R.string.resolving_anchor).toString();
            mDebugText.setText(s);
        });

        mCloudAnchorService.resolveCloudAnchor(cloudAnchorId, anchor -> {
            runOnUiThread(() -> {
                String s = mDebugText.getText() + "\n" + id + "\t"
                        + getResources().getText(R.string.anchor_resolved).toString();
                mDebugText.setText(s);
            });

            // Fix rotation for anchors
            float translations[] = new float[3];
            float rotations[] = new float[4];
            anchor.getPose().getTranslation(translations, 0);
            ArrayList<Plane> planes = new ArrayList<>(mArFragment.getArSceneView().getSession().getAllTrackables(Plane.class));
            planes.get(0).getCenterPose().getRotationQuaternion(rotations, 0);
            Anchor newAnchor = mArFragment.getArSceneView().getSession().createAnchor(new Pose(translations, rotations));

            mAnchorQueue.offer(new AnchorRenderable(id, model, newAnchor));
        });
    }

    private void addTransformableNode(Anchor anchor, int id, String model) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(mArFragment.getArSceneView().getScene());

//        AnchorNode textNode = new AnchorNode(anchor);
//        textNode.setLocalPosition(Vector3.add(textNode.getLocalPosition(), Vector3.up()));
//        textNode.setParent(mArFragment.getArSceneView().getScene());
//        ((TextView) mTextViewRenderable.getView()).setText(String.valueOf(id));
//        textNode.setRenderable(mTextViewRenderable);

        // Create the transformable model, and attach it to the anchor.
        TransformableNode transformableNode = new TransformableNode(mArFragment.getTransformationSystem());
        transformableNode.setParent(anchorNode);
        transformableNode.setRenderable(mModels.get(model).getRenderableModel());
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAugmentedLocationManager.pause();
        mCloudAnchorService = null;
        if (CloudAnchorServiceHelper.isBound()) {
            CloudAnchorServiceHelper.deinit(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        CloudAnchorServiceHelper.init(this, cloudAnchorService -> {
            mCloudAnchorService = cloudAnchorService;
            mAugmentedLocationManager.resume();

            Task.run(() -> {
                Session session = null;
                while (session == null) {
                    if (mArFragment.getArSceneView().getSession() == null) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {
                            Log.e(TAG, "Interrupted while waiting for Session!");
                        }
                        continue;
                    }

                    session = mArFragment.getArSceneView().getSession();
                    Config config = new Config(session);
                    config.setCloudAnchorMode(Config.CloudAnchorMode.ENABLED);
                    config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
                    session.configure(config);
                    mCloudAnchorService.setSession(session);
                }
            });
        });
    }
}
