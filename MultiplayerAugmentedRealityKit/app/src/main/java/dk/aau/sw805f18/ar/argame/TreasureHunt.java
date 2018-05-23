package dk.aau.sw805f18.ar.argame;


import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static java.util.stream.Collectors.toList;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.argame.location.AugmentedLocationManager;
import dk.aau.sw805f18.ar.common.helpers.SyncServiceHelper;
import dk.aau.sw805f18.ar.common.websocket.Packet;
import dk.aau.sw805f18.ar.services.SyncService;

public class TreasureHunt {
    private enum GameState {
        IDLE, STARTED, ONECHEST, FINISHED;
    }

    private static final String TAG = TreasureHunt.class.getSimpleName();
    private GameState mGameState;
    private ArGameActivity mActivity;
    private int mRandomChest;
    private int mChosenChest;
    private List<Integer> mChosenChests;
    private SyncService mSyncService;
    private Boolean mAwaitingResponse = false;
    private AugmentedLocationManager mManager;
    private Gson mGson;
    private int mRecivedRandomChest = 0;
    private boolean mAllPlayersAcked = false;
    private long mDelay;

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
        mAwaitingResponse = false;
        Button waitButton = mActivity.findViewById(R.id.Chest_Found_Btn);
        waitButton.setClickable(true);
        waitButton.setText("Open Chest");

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

            alert.setPositiveButton("Ok", (dialog, whichButton) -> {
            });
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

    //spillet virker efter hensigten men skal have multiplayer til at virke, det sidste der blev lavet var at
    //jeg prøvede at sende random pakken flere gange indtil at all har svaret tilbage med ack
    // ikke sikker på at de andre devices har fået random pakke
    public void startGame() {
        mGameState = GameState.STARTED;
        mChosenChests = new ArrayList<>();

        if (mSyncService.IsOwner()) {
            Log.i(TAG, String.valueOf(mManager.getAugmentedLocations().size()));
            assignRandomChestMaster(mManager.getAugmentedLocations());

            sendPacket(new Packet(Packet.RANDOM_CHEST, String.valueOf(mRandomChest)));
            mDelay = System.currentTimeMillis();
            Log.i(TAG, "send the random chest");
            mSyncService.getWebSocket().attachHandler(Packet.RANDOM_CHEST_ACK, (Packet packet) -> {
                mRecivedRandomChest += 1;
                Log.i(TAG,"team size: " + mSyncService.getPlayersOnTeam().size());
                Log.i(TAG, "randomChestCount: " + mRecivedRandomChest);
                Log.i(TAG,"recived random pack, waitng for for " + (mSyncService.getPlayersOnTeam().size() - mRecivedRandomChest) + " acks");
                if(mRecivedRandomChest == mSyncService.getPlayersOnTeam().size()) {
                    mAllPlayersAcked = true;
                }
            });
            mSyncService.getWebSocket().attachHandler(Packet.CHOSEN_CHEST_MASTER, (Packet packet) -> {
                int chosen = recieveChosenMasterPack(packet);
                if (chosen != -1) {
                    mAwaitingResponse = false;
                    Button waitButton = mActivity.findViewById(R.id.Chest_Found_Btn);
                    waitButton.setClickable(true);
                    waitButton.setText("Open Chest");
                    mChosenChest = chosen;
                }
            });

        } else {
            mSyncService.getWebSocket().attachHandler(Packet.CHOSEN_CHEST_SLAVE, packet -> {
                mChosenChest = receiveChosenSlavePacket(packet);
                actOnChosenPacket();
            });
            mSyncService.getWebSocket().attachHandler(Packet.RANDOM_CHEST, packet -> {
                mRandomChest = recieveRandomPacket(packet);
                Log.i(TAG,"Recived the random chest sending ack for packet");
                sendPacket(new Packet(Packet.RANDOM_CHEST_ACK, ""));
            });
            mSyncService.getWebSocket().attachHandler(Packet.RANDOM_CHEST_2, packet -> {
                mRandomChest = recieveRandomPacket(packet);
            });
        }
        Button firstChestBtn = mActivity.findViewById(R.id.Chest_Found_Btn);

        firstChestBtn.setOnClickListener(v -> {
            Log.i(TAG, "button clicked");
            Log.i(TAG, "Chosen chest: " + mChosenChest + " Random Chest: " + mRandomChest);
            if(mSyncService.IsOwner()) {
                mChosenChests.add(mChosenChest);
            } else {
                sendChosenPacket(new Packet(Packet.CHOSEN_CHEST_MASTER, mGson.toJson(mActivity.getAnchors().get(mChosenChest))));
            }
            firstChestBtn.setClickable(false);
            firstChestBtn.setText(R.string.waiting);
            mAwaitingResponse = true;
        });
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
        if (mGameState == GameState.IDLE) {
            startGame();
            Log.i(TAG, "Game started");
            return;
        }

        if ((mGameState != GameState.STARTED && mGameState != GameState.ONECHEST) || mAwaitingResponse) {
            Log.i(TAG, "returned because waiting or gamestate is idle");
            return;
        }
        if(!mAllPlayersAcked && mSyncService.IsOwner() && System.currentTimeMillis() - mDelay > 2000){
            return;
        } else if (!mAllPlayersAcked && mSyncService.IsOwner()){
            Log.i(TAG, "Not all acks recieved");
            sendPacket(new Packet(Packet.RANDOM_CHEST, String.valueOf(mRandomChest)));
            return;
        }

        double closestRange = 9999;
        Anchor tempChest = null;
        for (Anchor marker : mActivity.getAnchorsReverse().values()) {
            if (marker == null) {
                return;
            }

            double range = mActivity.calcPoseDistance(marker.getPose(), frame.getCamera().getPose());
            if (range < 10 && range < closestRange) {
                closestRange = range;
                tempChest = marker;

                setTestViewClose(closestRange, tempChest);
            }
        }
        if (tempChest != null) {
            mChosenChest = mActivity.getAnchors().get(tempChest);
        } else {

            setTestViewNone(frame);
            mChosenChest = -1;
        }
    }

    private void setTestViewNone(Frame frame) {
        if (mActivity.getAnchorsReverse().get(mRandomChest) == null) {
            return;

        }

        double distance = 9999;
        for (Anchor anchor : new ArrayList<>(mActivity.getAnchors().keySet())) {
            if (mActivity.calcPoseDistance(frame.getCamera().getDisplayOrientedPose(), anchor.getPose()) < distance) {
                distance = mActivity.calcPoseDistance(frame.getCamera().getDisplayOrientedPose(), anchor.getPose());
            }
        }

        double finalDistance = distance;
        mActivity.runOnUiThread(() -> {

            String s = "Distance to Anchor: " + finalDistance + "\n";
            Log.i(TAG, s);
            ((TextView) (mActivity.findViewById(R.id.debugText2))).setText(s);


            (mActivity.findViewById(R.id.Chest_Found_Btn)).setVisibility(View.INVISIBLE);
        });
    }

    private void setTestViewClose(double closestRange, Anchor closeChest) {
        mActivity.runOnUiThread(() -> {

            String s = "closest chest:" + closestRange;
            ((TextView) (mActivity.findViewById(R.id.debugText2))).setText(s);
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
        Log.i(TAG, "Random chest assigned to: " + random);
        mRandomChest = random;
    }

}

