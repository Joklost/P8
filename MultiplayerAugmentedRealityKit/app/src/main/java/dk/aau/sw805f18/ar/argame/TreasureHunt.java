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
import com.google.ar.sceneform.ux.TransformableNode;
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
import dk.aau.sw805f18.ar.common.websocket.WebSocketeer;
import dk.aau.sw805f18.ar.services.SyncService;

public class TreasureHunt {
    private enum GameState {
        IDLE, STARTED, ONECHEST, FINISHED;
    }

    private static final String TAG = TreasureHunt.class.getSimpleName();
    private GameState mGameState;
    private ArGameActivity mActivity;
    private int mCorrectChest;
    private int mClosestChest;
    private int mChosenChest;
    private List<Integer> mChosenChests;
    private SyncService mSyncService;
    private Boolean mAwaitingResponse = false;
    private AugmentedLocationManager mAugmentedLocationManager;
    private Gson mGson;
    private int mCorrectChestACKs = 0;
    private long mDelay;

    private Button mChestButton;
    private WebSocketeer mSocket;


    TreasureHunt(ArGameActivity activity, AugmentedLocationManager manager) {
        mSyncService = SyncServiceHelper.getInstance();

        mActivity = activity;
        mAugmentedLocationManager = manager;
        mGameState = GameState.IDLE;
        mGson = new Gson();
        mSocket = mSyncService.getWebSocket();
        mChestButton = mActivity.findViewById(R.id.Chest_Found_Btn);
    }

    public void startGame() {
        mGameState = GameState.STARTED;
        mChosenChests = new ArrayList<>();

        if (mSyncService.IsLeader()) {

            assignCorrectChest(mAugmentedLocationManager.getAugmentedLocations());
            sendPacket(new Packet(Packet.RANDOM_CHEST, String.valueOf(mCorrectChest)));
            mDelay = System.currentTimeMillis();

            mSocket.attachHandler(Packet.RANDOM_CHEST_ACK, (Packet packet) -> {
                mCorrectChestACKs += 1;
            });

            mSocket.attachHandler(Packet.CHOSEN_CHEST_LEADER, (Packet packet) -> {
                int chosen = receiveChosenLeaderPack(packet);
                if (chosen != -1) {
                    mAwaitingResponse = false;

                    mActivity.runOnUiThread(() -> {
                        mChestButton.setClickable(true);
                        mChestButton.setText(R.string.open_chest);
                    });
                }
            });

        } else {
            mSocket.attachHandler(Packet.CHOSEN_CHEST_PEER, packet -> {
                mChosenChest = receiveChosenPeerPacket(packet);
                actOnChosenPacket();
            });

            mSocket.attachHandler(Packet.RANDOM_CHEST, packet -> {
                mCorrectChest = recieveRandomPacket(packet);
                sendPacket(new Packet(Packet.RANDOM_CHEST_ACK, ""));
            });

            mSocket.attachHandler(Packet.RANDOM_CHEST_2, packet -> {
                mCorrectChest = recieveRandomPacket(packet);
            });
        }

        mChestButton.setOnClickListener(v -> {
            mActivity.runOnUiThread(() -> {
                mChestButton.setClickable(false);
                mChestButton.setText(R.string.waiting);
            });
            if (mSyncService.IsLeader()) {
                receiveChosenLeaderPack(null);
            } else {
                sendChosenPacket(new Packet(Packet.CHOSEN_CHEST_LEADER, String.valueOf(mClosestChest)));
                mAwaitingResponse = true;
            }
        });
    }

    public void onUpdate(Frame frame) {
        if (mGameState == GameState.IDLE) {
            startGame();
            return;
        }

        if ((mGameState != GameState.STARTED && mGameState != GameState.ONECHEST) || mAwaitingResponse) {
            return;
        }

        if (mSyncService.IsLeader() && mCorrectChestACKs != mSyncService.getPlayersOnTeam().size()) {

            if (System.currentTimeMillis() - mDelay < 3000) {
                return;
            }

            mDelay = System.currentTimeMillis();
            sendPacket(new Packet(Packet.RANDOM_CHEST, String.valueOf(mCorrectChest)));
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
            mClosestChest = mActivity.getAnchors().get(tempChest);
        } else {
            setTestViewNone(frame);
            mClosestChest = -1;
        }
    }

    private void sendPacket(Packet packet) {
        mSocket.send(packet);
    }

    private void sendChosenPacket(Packet packet) {
        mSocket.send(packet);
        mAwaitingResponse = true;
    }

    private int receiveChosenPeerPacket(Packet packet) {
        mAwaitingResponse = false;
        return Integer.parseInt(packet.Data);
    }

    private int receiveChosenLeaderPack(Packet packet) {
        if (packet == null) {
            mChosenChests.add(mClosestChest);
        } else {
            mChosenChests.add(Integer.parseInt(packet.Data));
        }

        if (mChosenChests.size() == mSyncService.getPlayersOnTeam().size()) {
            mSocket.send(new Packet(Packet.CHOSEN_CHEST_PEER, mGson.toJson(mostCommon(mChosenChests))));

            mChosenChest = mostCommon(mChosenChests);
            actOnChosenPacket();
            mChosenChests = new ArrayList<>();

            return mChosenChest;
        }
        return -1;
    }

    private int recieveRandomPacket(Packet packet) {
        return Integer.parseInt(packet.Data);
    }

    private void actOnChosenPacket() {
        mAwaitingResponse = false;
        AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);


        mActivity.runOnUiThread(() -> {
            mChestButton.setClickable(true);
            mChestButton.setText(R.string.open_chest);
        });
        if (mChosenChest == mCorrectChest) {
            Anchor anchor = mActivity.getAnchorsReverse().get(mCorrectChest);
            mActivity.runOnUiThread(() -> {
                mActivity.getTransformableNodes().get(anchor).setRenderable(mActivity.getModels().get("Chest").getRenderableModel());
            });

            if (!mSyncService.IsLeader()) {
                mChosenChest = -1;
                mCorrectChest = -1;
            } else {
                mChosenChests = new ArrayList<>();
            }

            if (mGameState == GameState.ONECHEST) {
                mGameState = GameState.FINISHED;
                alert.setTitle("Victory");
                alert.setMessage("You won the game!");
                alert.setPositiveButton("Ok", (dialog, whichButton) -> mActivity.finish());

            }
            if (mGameState == GameState.STARTED) {
                mGameState = GameState.ONECHEST;

                if (mSyncService.IsLeader()) {
                    List<Anchor> threeNearest = getThreeNearestChest();
                    assignCorrectChest(threeNearest);
                    mCorrectChestACKs = 0;
                }
                alert.setTitle("Right Chest");
                alert.setMessage("You found the correct chest!" + "\n" + "You must now find the next chest!");
                alert.setPositiveButton("Ok", (dialog, whichButton) -> {

                });
            }
        } else {

            alert.setTitle("Wrong Chest");
            alert.setMessage("You found the wrong chest!");

            if (mGameState == GameState.ONECHEST) {
                mActivity.runOnUiThread(() -> {
                    for (TransformableNode node : mActivity.getTransformableNodes().values()) {
                        node.setRenderable(mActivity.getModels().get("Treasure").getRenderableModel());
                    }
                });
                mGameState = GameState.STARTED;
                alert.setMessage("You selected the wrong chest!" + "\n" + "The game will now reset");
                if (mSyncService.IsLeader()) {
                    assignCorrectChest(mAugmentedLocationManager.getAugmentedLocations());
                    mCorrectChestACKs = 0;

                }
                mCorrectChest = -1;
            }

            alert.setPositiveButton("Ok", (dialog, whichButton) -> {
            });
        }
        mActivity.runOnUiThread(alert::show);
        mChosenChest = -1;
    }

    private List<Anchor> getThreeNearestChest() {
        List<Pair<Anchor, Double>> markers = new ArrayList<>(mActivity.getAnchors().keySet()).stream().map((marker) -> {
            Pose chosenChestPose = mActivity.getAnchorsReverse().get(mClosestChest).getPose();
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

    private void setTestViewNone(Frame frame) {
        if (mActivity.getAnchorsReverse().get(mCorrectChest) == null) {
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


            (mChestButton).setVisibility(View.INVISIBLE);
        });
    }

    private void setTestViewClose(double closestRange, Anchor closeChest) {
        mActivity.runOnUiThread(() -> {
            String s = "closest chest:" + closestRange;
            ((TextView) (mActivity.findViewById(R.id.debugText2))).setText(s);
            (mChestButton).setVisibility(View.VISIBLE);
        });
    }

    private void assignCorrectChest(Collection markers) {
        int random = new Random().nextInt(markers.size());
        mCorrectChest = random;
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


}

