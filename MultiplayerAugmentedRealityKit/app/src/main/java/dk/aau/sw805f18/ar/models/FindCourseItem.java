package dk.aau.sw805f18.ar.models;


public class FindCourseItem {
    private String mName;
    private int mPlayer, mAge;
    private double mDistance;

    public FindCourseItem(String name, int player, int age, double distance) {
        this.mName = name;
        this.mPlayer = player;
        this.mAge = age;
        this.mDistance = distance;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public int getPlayer() {
        return mPlayer;
    }

    public int getAge() {
        return mAge;
    }

    public void setAge(int age) {
        this.mAge = age;
    }

    public double getDistance() {
        return mDistance;
    }

    public void setDistance(double distance) {
        this.mDistance = distance;
    }
}
