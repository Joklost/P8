package dk.aau.sw805f18.ar.argame;


import android.support.v7.app.AlertDialog;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.gson.Gson;
import com.koushikdutta.async.http.WebSocket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.argame.location.AugmentedLocation;
import dk.aau.sw805f18.ar.argame.location.AugmentedLocationManager;
import dk.aau.sw805f18.ar.common.helpers.SyncServiceHelper;
import dk.aau.sw805f18.ar.common.websocket.Packet;
import dk.aau.sw805f18.ar.services.SyncService;

public class TreasureHunt {
    private enum GameState {
        IDLE, STARTED, ONECHEST, FINISHED;
    }

    private GameState mGameState;
    private ArGameActivity mActivity;
    private int mRandomChest;
    private int mChosenChest;
    private List<Integer> mChosenChests;
    private SyncService mSyncService;
    private Boolean mAwaitingResponse = false;
    private AugmentedLocationManager mManager;
    private Gson mGson;

    TreasureHunt(ArGameActivity activity, AugmentedLocationManager manager) {
        mSyncService = SyncServiceHelper.getInstance();

        mActivity = activity;
        mManager = manager;
        mGameState = GameState.IDLE;
        mGson = new Gson();
    }

    private void sendPacket(Packet packet) {
        mSyncService.getWebSocket().send(packet);
        //TODO slaves får pakker sendt af andre pakker
    }

    private int receiveChosenSlavePacket(Packet packet) {
        mAwaitingResponse = false;
        return Integer.parseInt(packet.Data);
    }

    private void actOnChosenPacket() {
        if (mChosenChest == (mRandomChest)) {
            AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
            if (mGameState == GameState.ONECHEST) {
                mGameState = GameState.FINISHED;
                alert.setTitle("Victory");
                alert.setMessage("You won the game!");
                alert.setPositiveButton("Ok", (dialog, whichButton) -> mActivity.finish());
            }
            if (mGameState == GameState.STARTED) {
                mGameState = GameState.ONECHEST;

                List<Anchor> threeNearest = getThreeNearestChest();
                assignRandomChestMaster(threeNearest);
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

            alert.setPositiveButton("Ok", (dialog, whichButton) -> {});
            alert.show();
        }
    }


    private void sendChosenPacket(Packet packet) {
        mSyncService.getWebSocket().send(packet);
        mAwaitingResponse = true;
    }

    private int recieveChosenMasterPack(Packet packet) {
        mChosenChests.add(mGson.fromJson(packet.Data, int.class));
        if (mChosenChests.size() == mSyncService.getPlayersOnTeam().size()) {
            mSyncService.getWebSocket().send(new Packet(Packet.RANDOM_CHEST, mGson.toJson(mostCommon(mChosenChests))));
            return mChosenChest = mostCommon(mChosenChests);
        }
        return -1;
    }


    public void startGame() {
        mGameState = GameState.STARTED;
        mChosenChests = new ArrayList<>();

        if (mSyncService.IsOwner()) {

            assignRandomChestMaster(mManager.getAugmentedLocations());
            sendPacket(new Packet(Packet.RANDOM_CHEST, mGson.toJson(mActivity.getAnchors().get(mRandomChest))));

            mSyncService.getWebSocket().attachHandler(Packet.CHOSEN_CHEST, (Packet packet) -> {
                int chosen = recieveChosenMasterPack(packet);
                if (chosen != -1) {
                    mChosenChest = chosen;
                }
            });

        } else {
            mSyncService.getWebSocket().attachHandler(Packet.CHOSEN_CHEST, packet -> {
                mChosenChest = receiveChosenSlavePacket(packet);
                actOnChosenPacket();
            });
            mSyncService.getWebSocket().attachHandler(Packet.RANDOM_CHEST, packet -> {
                mRandomChest = recieveRandomPacket(packet);
            });

            Button firstChestBtn = mActivity.findViewById(R.id.Chest_Found_Btn);

            firstChestBtn.setOnClickListener(v -> {
                sendChosenPacket(new Packet(Packet.CHOSEN_CHEST, mGson.toJson(mActivity.getAnchors().get(mChosenChest))));
                firstChestBtn.setClickable(false);
                firstChestBtn.setText(R.string.waiting);
            });
        }
    }

    private int recieveRandomPacket(Packet packet) {
        return mGson.fromJson(packet.Data, int.class);
    }


    private List<Anchor> getThreeNearestChest() {
        List<Pair<Anchor, Double>> markers = new ArrayList<>(mActivity.getAnchors().keySet()).stream().map((marker) -> {
            Pose chosenChestPose = mActivity.getAnchorsReverse().get(mChosenChest).getPose();
            Pose markerChestPose = marker.getPose();
            double distance = mActivity.calcPoseDistance(chosenChestPose, markerChestPose);
            return new Pair<>(marker, distance);
        }).sorted(Comparator.comparing(m -> m.second)).collect(toList());

        List<Anchor> threeNearest = new ArrayList<>();
        threeNearest.add(markers.get(1).first);
        if (markers.size() > 2) {
            threeNearest.add(markers.get(2).first);
        }
        if (markers.size() > 3) {
            threeNearest.add(markers.get(3).first);
        }
        return threeNearest;
    }

    public void onUpdate(Frame frame) {
        if(mGameState == GameState.IDLE) {
            startGame();
            return;
        }

        if ((mGameState != GameState.STARTED && mGameState != GameState.ONECHEST) || mAwaitingResponse) {
            return;
        }

        double closestRange = 9999;
        Anchor tempChest = null;
        for (Anchor marker : mActivity.getAnchorsReverse().values()) {
            if (marker == null) {
                return;
            }

            double range = mActivity.calcPoseDistance(marker.getPose(), frame.getCamera().getPose());
            if (range < 5 && range < closestRange) {
                closestRange = range;
                tempChest = marker;

                setTestViewClose(closestRange, tempChest);
            }
        }
        if (tempChest != null) {
            mChosenChest = mActivity.getAnchors().get(tempChest);
        }
        else {
            setTestViewNone();
            mChosenChest = -1;
        }
    }

    private void setTestViewNone() {
        if(mActivity.getAnchors().get(mRandomChest) == null){
            return;
        }
        String correctChest = mActivity.getAnchors().get(mRandomChest).toString();

        Anchor closestAnchor;
        double distance = 9999;
        for (Anchor anchor : new ArrayList<Anchor>(mActivity.getAnchors().keySet())) {
            if(mActivity.calcPoseDistance(anchor.getPose(), anchor.getPose())< distance) {
                closestAnchor = anchor;
                distance = mActivity.calcPoseDistance(anchor.getPose(), anchor.getPose());
            }
        }

        double finalDistance = distance;
        mActivity.runOnUiThread(() -> {

            String s = ((TextView) (mActivity.findViewById(R.id.debugText))).getText() + "\n"
                    + "Distance to Anchor: " + finalDistance + "\n";
            ((TextView) (mActivity.findViewById(R.id.debugText))).setText(s);


            (mActivity.findViewById(R.id.Chest_Found_Btn)).setVisibility(View.INVISIBLE);
        });
    }

    private void setTestViewClose(double closestRange, Anchor closeChest) {
        mActivity.runOnUiThread(() -> {



            ((TextView) (mActivity.findViewById(R.id.testView))).setText(String.format("Range to closest chest: %s", closestRange));
            String correctChest = mActivity.getAnchors().get(mRandomChest).toString();
            String closestChest = mActivity.getAnchors().get(closeChest).toString();
            ((TextView) (mActivity.findViewById(R.id.testView2))).setText(
                    String.format("Correct Chest: %s\nClosest Chest: %s\n", correctChest, closestChest)
            );
            (mActivity.findViewById(R.id.Chest_Found_Btn)).setVisibility(View.VISIBLE);

        });
    }

    private <T> T mostCommon(List<T> list) {
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

        return max != null ? max.getKey() : null;
    }

    private void assignRandomChestMaster(Collection markers) {
        int random = new Random().nextInt(markers.size());
        mRandomChest = random;
    }

}

