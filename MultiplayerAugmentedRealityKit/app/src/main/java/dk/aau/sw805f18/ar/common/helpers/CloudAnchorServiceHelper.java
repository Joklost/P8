package dk.aau.sw805f18.ar.common.helpers;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.function.Consumer;

import dk.aau.sw805f18.ar.services.CloudAnchorService;

public class CloudAnchorServiceHelper {
    private static CloudAnchorService sInstance;
    private static Consumer<CloudAnchorService> sOnBound;
    private static boolean sBound;

    public static CloudAnchorService getInstance() {
        return sInstance;
    }

    public static void init(Context context, Consumer<CloudAnchorService> onBound) {
        if (sInstance == null) {
            sOnBound = onBound;
            Intent intent = new Intent(context, CloudAnchorService.class);
            context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public static void deinit(Context context) {
        if (sInstance == null) {
            return;
        }

        if (sBound) {
            context.unbindService(mConnection);
        }
    }

    private static ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            CloudAnchorService.LocalBinder binder = (CloudAnchorService.LocalBinder) service;
            sInstance = binder.getService();
            sBound = true;
            sOnBound.accept(sInstance);

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            sBound = false;
        }
    };


    public static boolean isBound() {
        return sBound;
    }

}
