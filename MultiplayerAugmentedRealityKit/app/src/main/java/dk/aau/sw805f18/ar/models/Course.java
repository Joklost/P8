package dk.aau.sw805f18.ar.models;

import java.util.ArrayList;

public class Course {
    private String mName;
    private boolean mMapEnabled;
    private ArrayList<Game> mGames;

    //region Getters and setters
    public Course(String mName) {
        this.mName = mName;
    }

    public String getmName() {
        return mName;
    }

    public void setmName(String mName) {
        this.mName = mName;
    }

    public boolean ismMapEnabled() {
        return mMapEnabled;
    }

    public void setmMapEnabled(boolean mMapEnabled) {
        this.mMapEnabled = mMapEnabled;
    }

    public ArrayList<Game> getmGames() {
        return mGames;
    }

    public void setmGames(ArrayList<Game> mGames) {
        this.mGames = mGames;
    }
    //endregion

    /**
     * Overriding toString to custom format for listview items
     * @return Formatted string
     */
    public String toString() {
        return this.mName;
    }
}
