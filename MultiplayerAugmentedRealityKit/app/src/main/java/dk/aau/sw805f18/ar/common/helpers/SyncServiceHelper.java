package dk.aau.sw805f18.ar.common.helpers;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.function.Consumer;

import dk.aau.sw805f18.ar.services.SyncService;

public class SyncServiceHelper {
    private static SyncService sInstance;
    private static Consumer<SyncService> sOnBound;

    public static boolean isBound() {
        return sBound;
    }

    private static boolean sBound;

    public static SyncService getInstance() {
        return sInstance;
    }

    public static void init(Context context, Consumer<SyncService> onBound) {
        if (sInstance == null) {
            sOnBound = onBound;
            Intent intent = new Intent(context, SyncService.class);
            context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public static void deinit(Context context) {
        if (sInstance == null) {
            return;
        }

        sInstance.stopSelf();

        if (sBound) {
            context.unbindService(mConnection);
        }
    }

    private static ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SyncService.LocalBinder binder = (SyncService.LocalBinder) service;
            sBound = true;
            sInstance = binder.getService();
            sOnBound.accept(sInstance);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            sBound = false;
            sInstance.deinit();
        }
    };

}
