package dk.aau.sw805f18.ar.ar.location.sensor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import dk.aau.sw805f18.ar.common.helpers.ArLocationPermissionHelper;


public class DeviceLocation {
    //    private static final int TWO_MINUTES = 60 * 2 * 1000;
    private static final String TAG = DeviceLocation.class.getSimpleName();

    private static DeviceLocation sInstance;
    private static int sRefCount = 0;

    private Location mCurrentBestLocation;
    private FusedLocationProviderClient mLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }

            Location location = locationResult.getLastLocation();

            if (location == null) {
                return;
            }

            if (isBetterLocation(location)) {
                mCurrentBestLocation = location;
            }
        }
    };

    // Android Studio does not register the permission check from
    // the Permission Helper.
    @SuppressLint("MissingPermission")
    private DeviceLocation(Activity activity) {
        mLocationClient = LocationServices.getFusedLocationProviderClient(activity);
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (!ArLocationPermissionHelper.hasPermission(activity)) {
            ArLocationPermissionHelper.requestPermission(activity);
            Log.e(TAG, "Missing Location Permissions");
            return;
        }
        resume();
    }

    public Location getCurrentBestLocation() {
        return mCurrentBestLocation;
//        return BuildLocation(57.01968, 9.988778);
    }

    @SuppressLint("MissingPermission")
    public void resume() {
        sRefCount++;
        mLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
    }

    public void pause() {
        synchronized (DeviceLocation.class) {
            sRefCount--;
            if (sRefCount == 0) {
                mLocationClient.removeLocationUpdates(mLocationCallback);
            }
        }
    }

    public static Location BuildLocation(double latitude, double longitude) {
        Location loc = new Location("");
        loc.setLatitude(latitude);
        loc.setLongitude(longitude);
        return loc;
    }

    private boolean isBetterLocation(Location location) {
        // TODO: Find a better way to check if location is better.
        return mCurrentBestLocation == null || mCurrentBestLocation.getAccuracy() >= location.getAccuracy();
    }

    public static DeviceLocation getInstance(Activity activity) {
        if (sInstance == null) {
            // Double checked locking makes sure that
            // the singleton is thread-safe.
            synchronized (DeviceLocation.class) {
                if (sInstance == null) {
                    sInstance = new DeviceLocation(activity);
                }
            }
            sInstance = new DeviceLocation(activity);
        }
        return sInstance;
    }

    @Override
    public Object clone() throws CloneNotSupportedException{
        throw new CloneNotSupportedException("Singleton, cannot be clonned");
    }
}