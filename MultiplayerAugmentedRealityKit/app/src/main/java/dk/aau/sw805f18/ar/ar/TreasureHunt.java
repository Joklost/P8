package dk.aau.sw805f18.ar.ar;


import android.widget.TextView;

import com.google.ar.core.Frame;

import java.util.Random;

import dk.aau.sw805f18.ar.R;
import dk.aau.sw805f18.ar.ar.location.LocationMarker;
import dk.aau.sw805f18.ar.ar.location.LocationScene;

public class TreasureHunt {

    private GameState mGameState;
    private ArActivity mArActivity;
    private LocationScene mScene;
    private LocationMarker randomChest;
    private LocationMarker chosenChest;
    private double mClosestChest;

    public TreasureHunt(ArActivity activity, LocationScene scene) {
        mGameState = GameState.STARTED;
        mArActivity = activity;
        mScene = scene;
    }

    public void StartGame() {
        //need to make sure the activity has started;
        mArActivity.fetchTreasureHuntModels();
        AssignRandomChest();

    }

    public void gameLoop(Frame frame) {

        if (mGameState == GameState.STARTED) {
            double closestRange = 9999;
            LocationMarker tempChest = null;
            for (LocationMarker marker : mScene.getLocationMarkers()) {
                double range = mArActivity.calcPoseDistance(marker.getAnchor().getPose(), frame.getCamera().getPose());
                if (range < 5 && range < closestRange) {
                    closestRange = range;
                    chosenChest = marker;
                    double finalClosestRange = closestRange;
                    mArActivity.runOnUiThread(() -> {
                        ((TextView)(mArActivity.findViewById(R.id.testView))).setText("Range to closest chest: " + finalClosestRange);
                    });
                }
                else {
                    ((TextView)(mArActivity.findViewById(R.id.testView))).setText("No chests nearby");

                }
            }
            if (tempChest != null) {
                chosenChest = tempChest;
            }
        }
    }

    private void AssignRandomChest() {
        randomChest = mScene.getLocationMarkers().get(new Random().nextInt(mScene.getLocationMarkers().size()));

    }
}

