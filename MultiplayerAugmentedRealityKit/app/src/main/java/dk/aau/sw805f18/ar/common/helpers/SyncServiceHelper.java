package dk.aau.sw805f18.ar.common.helpers;


import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import dk.aau.sw805f18.ar.services.SyncService;

public class SyncServiceHelper {
    private static SyncService mInstance;

    public static SyncService getInstance() {
        return mInstance;
    }

    public static void init(Context context) {
        if (mInstance == null) {
            Intent intent = new Intent(context, SyncService.class);
            context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public static void stop() {
        mInstance.stopSelf();
    }

    private static ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SyncService.LocalBinder binder = (SyncService.LocalBinder) service;
            mInstance = binder.getService();
            mInstance.init();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mInstance.deinit();
        }
    };

}
