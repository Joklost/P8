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
    private Anchor mRandomChest;
    private Anchor mChosenChest;
    private List<Anchor> mChosenChests;
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
        mSyncService.getWebSocketeerServer().sendToAll(packet);
    }

    private Anchor receiveChosenSlavePacket(Packet packet) {
        mAwaitingResponse = false;
        return mActivity.getAnchorsReverse().get(mGson.fromJson(packet.Data, int.class));
    }

    private void actOnChosenPacket() {
        if (mChosenChest.equals(mRandomChest)) {
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

    private Anchor recieveChosenMasterPack(Packet packet) {
        mChosenChests.add(mGson.fromJson(packet.Data, Anchor.class));
        if (mChosenChests.size() == mSyncService.getPlayersOnTeam().size()) {
            mSyncService.getWebSocketeerServer().sendToAll(new Packet(Packet.RANDOM_CHEST, mGson.toJson(mostCommon(mChosenChests))));
            return mChosenChest = mostCommon(mChosenChests);
        }
        return null;
    }


    public void startGame() {
        mGameState = GameState.STARTED;
        mChosenChests = new ArrayList<>();

        if (mSyncService.IsOwner()) {

            assignRandomChestMaster(new ArrayList<>(mActivity.getAnchors().keySet()));
            sendPacket(new Packet(Packet.RANDOM_CHEST, mGson.toJson(mActivity.getAnchors().get(mRandomChest))));

            mSyncService.getWebSocketeerServer().attachHandler(Packet.CHOSEN_CHEST, (WebSocket ws, Packet packet) -> {
                Anchor chosen = recieveChosenMasterPack(packet);
                if (chosen != null) {
                    mChosenChest = chosen;
                }
            });

        } else {
            mSyncService.getWifiP2pSocket().attachHandler(Packet.CHOSEN_CHEST, packet -> {
                mChosenChest = receiveChosenSlavePacket(packet);
                actOnChosenPacket();
            });
            mSyncService.getWifiP2pSocket().attachHandler(Packet.RANDOM_CHEST, packet -> {
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

    private Anchor recieveRandomPacket(Packet packet) {
        return mActivity.getAnchorsReverse().get(mGson.fromJson(packet.Data, int.class));
    }


    private List<Anchor> getThreeNearestChest() {
        List<Pair<Anchor, Double>> markers = new ArrayList<>(mActivity.getAnchors().keySet()).stream().map((marker) -> {
            Pose chosenChestPose = mChosenChest.getPose();
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

    public void update(Frame frame) {
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
            mChosenChest = tempChest;
        }
        else if(mGameState == GameState.IDLE) {
            startGame();
        }
        else {
            setTestViewNone();
            mChosenChest = null;
        }
    }

    private void setTestViewNone() {
        String correctChest = mActivity.getAnchors().get(mRandomChest).toString();
        mActivity.runOnUiThread(() -> {
            ((TextView) (mActivity.findViewById(R.id.testView))).setText(R.string.no_chest);
            ((TextView) (mActivity.findViewById(R.id.testView2))).setText(
                    String.format("Correct Chest: %s\nClosest Chest: None\n", correctChest)
            );

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

    private void assignRandomChestMaster(List<Anchor> markers) {
        mRandomChest = markers.get(new Random().nextInt(markers.size()));
    }
}

