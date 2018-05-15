package dk.aau.sw805f18.ar.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerSocketService extends Service {
    private static final String TAG = SyncService.class.getSimpleName();
    private String mName;
    private int mPort;
    private final IBinder mBinder = new ServerSocketService.LocalBinder();
    private Socket mSocket;
    private InputStream mInStream;
    private OutputStream mOutStream;

    public ServerSocketService(String name, int port) {
        mName = name;
        mPort = port;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public ServerSocketService getService() {
            return ServerSocketService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            mSocket = new ServerSocket(mPort).accept();
            mInStream = mSocket.getInputStream();
            mOutStream = mSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        byte[] buff = new byte[32];
        int bytes;

        // TODO: fix this so loop can be stopped
        while (true) {
            try {
                bytes = mInStream.read(buff);
                Log.i(TAG, "WiFi data received: " + buff.toString());
                // TODO: Do something with input.
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
