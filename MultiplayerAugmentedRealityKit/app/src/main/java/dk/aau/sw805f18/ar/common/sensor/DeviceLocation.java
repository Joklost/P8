package dk.aau.sw805f18.ar.common.sensor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import dk.aau.sw805f18.ar.common.helpers.ArLocationPermissionHelper;


public class DeviceLocation {
    private static final String TAG = DeviceLocation.class.getSimpleName();

    private static DeviceLocation sInstance;
    private static int sRefCount = 0;
    private final Activity mActivity;

    private Location mCurrentBestLocation;
    private FusedLocationProviderClient mLocationClient;
    private LocationRequest mLocationRequest;

    private Map<Context, Consumer<Location>> mSubscribers;

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
                for (Consumer<Location> subscriber : mSubscribers.values()) {
                    subscriber.accept(mCurrentBestLocation);
                }
            }
        }
    };

    // Android Studio does not register the permission check from
    // the Permission Helper.
    @SuppressLint("MissingPermission")
    private DeviceLocation(Activity activity) {
        mActivity = activity;

        LocationManager lm = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);
        if (lm != null && !lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            promptUserToEnableGps();
        }

        mSubscribers = new HashMap<>();
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

    public static Location BuildLocation(double latitude, double longitude) {
        Location loc = new Location("");
        loc.setLatitude(latitude);
        loc.setLongitude(longitude);
        return loc;
    }

    public static DeviceLocation getInstance(Activity activity) {
        if (sInstance == null) {
            sInstance = new DeviceLocation(activity);
        }
        return sInstance;
    }

    private void promptUserToEnableGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setMessage("Please enable location services")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, id) -> mActivity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("No", (dialog, id) -> dialog.cancel());
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public Location getCurrentBestLocation() {
        return mCurrentBestLocation;
    }

    public void subscribe(Context context, Consumer<Location> onUpdate) {
        mSubscribers.put(context, onUpdate);
    }

    public void unsubscribe(Context context) {
        mSubscribers.remove(context);
    }

    @SuppressLint("MissingPermission")
    public void resume() {
        sRefCount++;
        mLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
    }

    public void pause() {
        sRefCount--;
        if (sRefCount == 0) {
            mLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    private boolean isBetterLocation(Location location) {
        return mCurrentBestLocation == null || mCurrentBestLocation.getAccuracy() >= location.getAccuracy();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Singleton, cannot be cloned");
    }
}