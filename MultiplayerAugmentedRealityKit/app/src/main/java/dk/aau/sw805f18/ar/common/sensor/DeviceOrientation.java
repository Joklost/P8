package dk.aau.sw805f18.ar.common.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;


public class DeviceOrientation implements SensorEventListener {
    private static DeviceOrientation sInstance;
    private static int sRefCount = 0;

    private SensorManager mSensorManager;

    // Gravity rotational data
    private float gravity[];

    // Magnetic rotational data
    private float magnetic[];
    private float accels[] = new float[3];
    private float mags[] = new float[3];
    private float[] values = new float[3];
    private float azimuth;
    private float mPitch;
    public float roll;

    // North
    private float mCurrentDegree = 0f;

    private DeviceOrientation(Context context) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // Get the device heading
        float degree = -Math.round( event.values[0] );

        // Temporary fix until we can work out what's causing the anomalies
        if(degree != 1.0 && degree != 0 && degree != 2.0 && degree != -1.0)
            mCurrentDegree = degree;

        switch (event.sensor.getType()) {
            case Sensor.TYPE_MAGNETIC_FIELD:
                mags = event.values.clone();
                break;
            case Sensor.TYPE_ACCELEROMETER:
                accels = event.values.clone();
                break;
        }

        if (mags != null && accels != null) {
            gravity = new float[9];
            magnetic = new float[9];
            SensorManager.getRotationMatrix(gravity, magnetic, accels, mags);
            float[] outGravity = new float[9];
            SensorManager.remapCoordinateSystem(gravity, SensorManager.AXIS_X,SensorManager.AXIS_Z, outGravity);
            SensorManager.getOrientation(outGravity, values);

            azimuth = values[0] * 57.2957795f;
            mPitch = values[1] * 57.2957795f;
            roll = values[2] * 57.2957795f;
            mags = null;
            accels = null;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public void resume() {
        sRefCount++;
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void pause() {
        synchronized (DeviceLocation.class) {
            sRefCount--;
            if (sRefCount == 0) {
                mSensorManager.unregisterListener(this);
            }
        }
    }

    public static DeviceOrientation getInstance(Context context) {
        if (sInstance == null) {
            // Double checked locking makes sure that
            // the singleton is thread-safe.
            synchronized (DeviceOrientation.class) {
                if (sInstance == null) {
                    sInstance = new DeviceOrientation(context);
                }
            }
            sInstance = new DeviceOrientation(context);
        }
        return sInstance;
    }

    @Override
    public Object clone() throws CloneNotSupportedException{
        throw new CloneNotSupportedException("Singleton, cannot be clonned");
    }

    public float getPitch() {
        return mPitch;
    }

    public float getCurrentDegree() {
        return mCurrentDegree;
    }
}
