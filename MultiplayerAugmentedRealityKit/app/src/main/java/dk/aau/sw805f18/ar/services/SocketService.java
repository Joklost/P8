package dk.aau.sw805f18.ar.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class SocketService extends IntentService {
    private static final String TAG = SyncService.class.getSimpleName();
    private String mName;
    private Socket mSocket;
    private InetSocketAddress mAddress;
    private final IBinder mBinder = new SocketService.LocalBinder();
    private InputStream mInStream;
    private OutputStream mOutStream;

    public SocketService(String name, InetSocketAddress address) {
        super(name);
        mName = name;
        mAddress = address;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        // data format = [lat, lon, rot, sca, cpi]
        byte[] data = intent.getByteArrayExtra("data");

        try {
            mOutStream.write(data);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (mSocket != null) return mBinder;

        try {
            mSocket = new Socket();

            mSocket.bind(null);
            mSocket.connect(mAddress, 5000);

            mInStream = mSocket.getInputStream();
            mOutStream = mSocket.getOutputStream();

            Log.i(TAG + " " + mName, "Socket connected. Input- and Outputstream connected.");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mBinder;
    }

    public class LocalBinder extends Binder {
        public SocketService getService() {
            return SocketService.this;
        }
    }
}
