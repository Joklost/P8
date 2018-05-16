package dk.aau.sw805f18.ar.ar;


import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static java.util.stream.Collectors.toList;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.ar.location.LocationMarker;
import dk.aau.sw805f18.ar.ar.location.LocationScene;
import dk.aau.sw805f18.ar.common.helpers.SyncServiceHelper;
import dk.aau.sw805f18.ar.common.rendering.AnnotationRenderer;
import dk.aau.sw805f18.ar.common.websocket.Packet;
import dk.aau.sw805f18.ar.services.SyncService;

public class TreasureHunt {

    private GameState mGameState;
    private ArActivity mArActivity;
    private LocationScene mScene;
    private LocationMarker randomChest;
    private LocationMarker chosenChest;
    private List<LocationMarker> mChosenChests;
    private SyncService mSyncService;
    private Boolean awaitingResponse = false;

    public TreasureHunt(ArActivity activity, LocationScene scene) {
        mGameState = GameState.STARTED;
        mArActivity = activity;
        mScene = scene;
    }


    public void StartGame() {
        //need to make sure the activity has started;
        //mArActivity.fetchTreasureHuntModels();
        mChosenChests = new ArrayList<>();
        Gson gson = new Gson();
        mSyncService = SyncServiceHelper.getInstance();

        if (SyncServiceHelper.getInstance().isHostingWifiP2p()) {
            AssignRandomChestMaster(mScene.getLocationMarkers());

            mSyncService.getWebSocketeerServer().sendToAll(new Packet(Packet.RANDOM_CHEST, gson.toJson(randomChest)));

            mSyncService.getWebSocketeerServer().attachHandler(Packet.CHOSEN_CHEST, (ws, packet) -> {
                mChosenChests.add(gson.fromJson(packet.Data, LocationMarker.class));
                if (mChosenChests.size() == mSyncService.getWebSocketeerServer().getConnectedDevices()) {
                    mSyncService.getWebSocketeerServer().sendToAll(new Packet(Packet.RANDOM_CHEST, gson.toJson(mostCommon(mChosenChests))));
                }
            });

        } else {
            mSyncService.getWebSocket().attachHandler(Packet.CHOSEN_CHEST, packet -> {
                chosenChest = gson.fromJson(packet.Data, LocationMarker.class);
                if (chosenChest.equals(randomChest)) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(mArActivity);
                    if (mGameState == GameState.ONECHEST) {
                        mGameState = GameState.FINISHED;
                        alert.setTitle("Victory");
                        alert.setMessage("You won the game!");
                        alert.setPositiveButton("Ok", (dialog, whichButton) -> mArActivity.finish());
                    }
                    if (mGameState == GameState.STARTED) {
                        mGameState = GameState.ONECHEST;

                        List<LocationMarker> threeNearest = getThreeNearestChest();
                        AssignRandomChestMaster(threeNearest);
                        alert.setTitle("Right Chest");
                        alert.setMessage("You found the correct chest!" + "\n" + "You must now find the next chest!");
                        alert.setPositiveButton("Ok", (dialog, whichButton) -> {

                        });
                    }

                    alert.show();
                } else {
                    AlertDialog.Builder alert = new AlertDialog.Builder(mArActivity);
                    alert.setTitle("Wrong Chest");
                    alert.setMessage("You found the wrong chest!");

                    if (mGameState == GameState.ONECHEST) {
                        mGameState = GameState.STARTED;
                        alert.setMessage("You selected the wrong chest!" + "\n" + "The game will now reset");
                    }

                    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                        }
                    });
                    alert.show();
                }


            });
            mSyncService.getWebSocket().attachHandler(Packet.RANDOM_CHEST, packet -> {
                randomChest = gson.fromJson(packet.Data, LocationMarker.class);
            });


            Button firstChestBtn = mArActivity.findViewById(R.id.Chest_Found_Btn);


            firstChestBtn.setOnClickListener(v -> {
                mSyncService.getWebSocket().send(new Packet(Packet.CHOSEN_CHEST, gson.toJson(chosenChest)));
                firstChestBtn.setClickable(false);
                firstChestBtn.setText("Waiting...");
            });
        }
    }


    private List<LocationMarker> getThreeNearestChest() {
        List<Pair<LocationMarker, Double>> markers = mScene.getLocationMarkers().stream().map((marker) -> {
            Pose chosenChestPose = chosenChest.getAnchor().getPose();
            Pose markerChestPose = marker.getAnchor().getPose();
            double distance = mArActivity.calcPoseDistance(chosenChestPose, markerChestPose);
            return new Pair<>(marker, distance);
        }).collect(toList());
        Collections.sort(markers, Comparator.comparing(m -> m.second));

        List<LocationMarker> threeNearest = new ArrayList<>();
        threeNearest.add(markers.get(1).first);
        if (markers.size() > 2) {
            threeNearest.add(markers.get(2).first);
        }
        if (markers.size() > 3) {
            threeNearest.add(markers.get(3).first);
        }
        return threeNearest;
    }

    @SuppressLint("SetTextI18n")
    public void gameLoop(Frame frame) {

        if (mGameState == GameState.STARTED || mGameState == GameState.ONECHEST) {
            double closestRange = 9999;
            LocationMarker tempChest = null;
            for (LocationMarker marker : mScene.getLocationMarkers()) {
                if (marker.getAnchor() == null) {
                    return;
                }
                double range = mArActivity.calcPoseDistance(marker.getAnchor().getPose(), frame.getCamera().getPose());
                if (range < 5 && range < closestRange) {
                    closestRange = range;
                    tempChest = marker;

                    double finalClosestRange = closestRange;
                    mArActivity.runOnUiThread(() -> {

                        ((TextView) (mArActivity.findViewById(R.id.testView))).setText("Range to closest chest: " + finalClosestRange);
                        String correctChest = ((AnnotationRenderer) randomChest.getRenderer()).getmAnnotationText();
                        String closestChest = ((AnnotationRenderer) chosenChest.getRenderer()).getmAnnotationText();
                        ((TextView) (mArActivity.findViewById(R.id.testView2))).setText(
                                "Correct Chest: " + correctChest + "\n" +
                                        "Closest Chest: " + closestChest + "\n"
                        );
                        (mArActivity.findViewById(R.id.Chest_Found_Btn)).setVisibility(View.VISIBLE);

                    });
                }
            }
            if (tempChest != null) {
                chosenChest = tempChest;
            } else {
                String correctChest = ((AnnotationRenderer) randomChest.getRenderer()).getmAnnotationText();
                mArActivity.runOnUiThread(() -> {
                    ((TextView) (mArActivity.findViewById(R.id.testView))).setText("No Chest Nearby");
                    ((TextView) (mArActivity.findViewById(R.id.testView2))).setText(
                            "Correct Chest: " + correctChest + "\n" +
                                    "Closest Chest: " + "None" + "\n");


                    (mArActivity.findViewById(R.id.Chest_Found_Btn)).setVisibility(View.INVISIBLE);
                });
                chosenChest = null;
            }
        }
    }

    public static <T> T mostCommon(List<T> list) {
        Map<T, Integer> map = new HashMap<>();

        for (T t : list) {
            Integer val = map.get(t);
            map.put(t, val == null ? 1 : val + 1);
        }

        Map.Entry<T, Integer> max = null;

        for (Map.Entry<T, Integer> e : map.entrySet()) {
            if (max == null || e.getValue() > max.getValue())
                max = e;
        }

        return max.getKey();
    }

    private void AssignRandomChestMaster(List<LocationMarker> markers) {
        randomChest = markers.get(new Random().nextInt(markers.size()));
    }
}

