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
import com.koushikdutta.async.http.WebSocket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static java.util.stream.Collectors.toList;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.argame.ArGameActivity;
import dk.aau.sw805f18.ar.argame.location.AugmentedLocation;
import dk.aau.sw805f18.ar.argame.location.AugmentedLocationManager;
import dk.aau.sw805f18.ar.common.helpers.SyncServiceHelper;
import dk.aau.sw805f18.ar.common.websocket.Packet;
import dk.aau.sw805f18.ar.services.SyncService;

public class TreasureHunt {

    private GameState mGameState;
    private ArGameActivity mActivity;
    private AugmentedLocation randomChest;
    private AugmentedLocation chosenChest;
    private List<AugmentedLocation> mChosenChests;
    private SyncService mSyncService;
    private Boolean awaitingResponse = false;
    private AugmentedLocationManager mManager;
    private Gson gson;

    public TreasureHunt(ArGameActivity activity, AugmentedLocationManager manager) {
        mGameState = GameState.STARTED;
        mSyncService = SyncServiceHelper.getInstance();


        mActivity = activity;
        mManager = manager;
        gson = new Gson();
    }

    private void sendRandomPack(Packet packet) {
        mSyncService.getWebSocketeerServer().sendToAll(packet);
    }

    private AugmentedLocation recieveChosenSlavePack(Packet packet) {
        awaitingResponse = false;
        return gson.fromJson(packet.Data, AugmentedLocation.class);
    }

    private void actOnChosenPack() {
        if (chosenChest.equals(randomChest)) {
            AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
            if (mGameState == GameState.ONECHEST) {
                mGameState = GameState.FINISHED;
                alert.setTitle("Victory");
                alert.setMessage("You won the game!");
                alert.setPositiveButton("Ok", (dialog, whichButton) -> mActivity.finish());
            }
            if (mGameState == GameState.STARTED) {
                mGameState = GameState.ONECHEST;

                List<AugmentedLocation> threeNearest = getThreeNearestChest();
                AssignRandomChestMaster(threeNearest);
                alert.setTitle("Right Chest");
                alert.setMessage("You found the correct chest!" + "\n" + "You must now find the next chest!");
                alert.setPositiveButton("Ok", (dialog, whichButton) -> {

                });
            }

            alert.show();
        } else {
            AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
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
    }



    private void sendChosenPack(Packet packet) {
        mSyncService.getWifiP2pSocket().send(packet);
        awaitingResponse = true;
    }

    private AugmentedLocation recieveChosenMasterPack(Packet packet) {
        mChosenChests.add(gson.fromJson(packet.Data, AugmentedLocation.class));
        if (mChosenChests.size() == mSyncService.getWebSocketeerServer().getConnectedDevices()) {
            mSyncService.getWebSocketeerServer().sendToAll(new Packet(Packet.RANDOM_CHEST, gson.toJson(mostCommon(mChosenChests))));
            return chosenChest = mostCommon(mChosenChests);
        }
        return null;
    }


    public void StartGame() {
        //need to make sure the activity has started;
        //mArActivity.fetchTreasureHuntModels();

        mChosenChests = new ArrayList<>();

        if (mSyncService.isHostingWifiP2p()) {
            AssignRandomChestMaster(mManager.getAugmentedLocations());

            sendRandomPack(new Packet(Packet.RANDOM_CHEST, gson.toJson(randomChest)));

            mSyncService.getWebSocketeerServer().attachHandler(Packet.CHOSEN_CHEST, (WebSocket ws, Packet packet) -> {
                AugmentedLocation chosen = recieveChosenMasterPack(packet);
                if(chosen != null) {
                    chosenChest = chosen;
                }
            });

        } else {
            mSyncService.getWifiP2pSocket().attachHandler(Packet.CHOSEN_CHEST, packet -> {
                recieveChosenSlavePack(packet);
                actOnChosenPack();
            });
            mSyncService.getWifiP2pSocket().attachHandler(Packet.RANDOM_CHEST, packet -> {
                randomChest = recieveRandomPack(packet);

            });


            Button firstChestBtn = mActivity.findViewById(R.id.Chest_Found_Btn);


            firstChestBtn.setOnClickListener(v -> {
                sendChosenPack(new Packet(Packet.CHOSEN_CHEST, gson.toJson(chosenChest)));
                firstChestBtn.setClickable(false);
                firstChestBtn.setText("Waiting...");
            });
        }
    }

    private AugmentedLocation recieveRandomPack(Packet packet) {
        return gson.fromJson(packet.Data, AugmentedLocation.class);
    }


    private List<AugmentedLocation> getThreeNearestChest() {
        List<Pair<AugmentedLocation, Double>> markers = mManager.getAugmentedLocations().stream().map((marker) -> {
            Pose chosenChestPose = chosenChest.getAnchor().getPose();
            Pose markerChestPose = marker.getAnchor().getPose();
            double distance = mActivity.calcPoseDistance(chosenChestPose, markerChestPose);
            return new Pair<>(marker, distance);
        }).collect(toList());
        Collections.sort(markers, Comparator.comparing(m -> m.second));

        List<AugmentedLocation> threeNearest = new ArrayList<>();
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

        if ((mGameState == GameState.STARTED || mGameState == GameState.ONECHEST) && !awaitingResponse) {
            double closestRange = 9999;
            AugmentedLocation tempChest = null;
            for (AugmentedLocation marker : mManager.getAugmentedLocations()) {
                if (marker.getAnchor() == null) {
                    return;
                }
                double range = mActivity.calcPoseDistance(marker.getAnchor().getPose(), frame.getCamera().getPose());
                if (range < 5 && range < closestRange) {
                    closestRange = range;
                    tempChest = marker;

                    setTestViewClose(closestRange, tempChest);

                }
            }
            if (tempChest != null) {
                chosenChest = tempChest;
            } else {
                setTestViewNone();
                chosenChest = null;
            }
        } else {

        }
    }

    private void setTestViewNone() {
        String correctChest = randomChest.getLocation().getLatitude() + " " + randomChest.getLocation().getLongitude();
        mActivity.runOnUiThread(() -> {
            ((TextView) (mActivity.findViewById(R.id.testView))).setText("No Chest Nearby");
            ((TextView) (mActivity.findViewById(R.id.testView2))).setText(
                    "Correct Chest: " + correctChest + "\n" +
                            "Closest Chest: " + "None" + "\n");


            (mActivity.findViewById(R.id.Chest_Found_Btn)).setVisibility(View.INVISIBLE);
        });
    }

    private void setTestViewClose(double closestRange , AugmentedLocation closeChest) {
        mActivity.runOnUiThread(() -> {

            ((TextView) (mActivity.findViewById(R.id.testView))).setText("Range to closest chest: " + closestRange);
            String correctChest = randomChest.getLocation().getLatitude() + " " + randomChest.getLocation().getLongitude();
            String closestChest = closeChest.getLocation().getLatitude() + " " + closeChest.getLocation().getLongitude();
            ((TextView) (mActivity.findViewById(R.id.testView2))).setText(
                    "Correct Chest: " + correctChest + "\n" +
                            "Closest Chest: " + closestChest + "\n"
            );
            (mActivity.findViewById(R.id.Chest_Found_Btn)).setVisibility(View.VISIBLE);

        });
    }
    public <T> T mostCommon(List<T> list) {
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

    private void AssignRandomChestMaster(List<AugmentedLocation> markers) {
        randomChest = markers.get(new Random().nextInt(markers.size()));
    }
}

