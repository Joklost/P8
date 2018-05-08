package dk.aau.sw805f18.ar.ar.location.sensor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import dk.aau.sw805f18.ar.ar.location.LocationScene;
import dk.aau.sw805f18.ar.ar.location.utils.ARLocationPermissionHelper;


/**
 * Created by John on 02/03/2018.
 */

public class DeviceLocation {
    private static final int TWO_MINUTES = 60 * 2 * 1000;
    private static final String TAG = DeviceLocation.class.getSimpleName();

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

            Log.i(TAG, "Found location: " + location.toString());

            if (isBetterLocation(location)){
                mCurrentBestLocation = location;
            }
        }
    };

    // Android Studio does not register the permission check from
    // the Permission Helper.
    @SuppressLint("MissingPermission")
    public DeviceLocation(Activity activity) {
        mLocationClient = LocationServices.getFusedLocationProviderClient(activity);
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (!ARLocationPermissionHelper.hasPermission(activity)) {
            Log.e(TAG, "Missing Location Permissions");
            activity.finish();
        }

        resume();
    }

    public Location getCurrentBestLocation() {
        return mCurrentBestLocation;
//        return BuildLocation(57.01968, 9.988778);
    }

    @SuppressLint("MissingPermission")
    public void resume() {
        mLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
    }

    public void pause() {
        mLocationClient.removeLocationUpdates(mLocationCallback);
    }

    public static Location BuildLocation(double latitude, double longitude) {
        Location loc = new Location("");
        loc.setLatitude(latitude);
        loc.setLongitude(longitude);
        return loc;
    }

    protected boolean isBetterLocation(Location location) {
        if (mCurrentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - mCurrentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - mCurrentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                mCurrentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } //else if (mCurrentBestLocation.distanceTo(location) > 5) {
//            return true;
//        }
        //else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            //return true;
        //}
        return false;
    }

    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }



}