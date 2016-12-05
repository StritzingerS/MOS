package at.fhooe.mc.mos.hardware;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import at.fhooe.mc.mos.logic.AltitudeObserver;

/**
 * Accessing barometer sensor of phone to get altitude.
 */
public class AndroidBarometer implements AltitudeMonitor, SensorEventListener {
    private static final String TAG = AndroidBarometer.class.getSimpleName();

    private List<AltitudeObserver> mObservers;
    private static AndroidBarometer INSTANCE;
    private Context mContext;
    private SensorManager mSensorManager;

    private float mLastAltitude;

    public static AndroidBarometer getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new AndroidBarometer(context);
        }
        return INSTANCE;
    }

    private AndroidBarometer(Context context) {
        mObservers = new ArrayList<>();
        mContext = context;
        mLastAltitude = -1;
    }


    private void registerSensorListeners() {
        mSensorManager = (SensorManager) mContext.getSystemService(mContext.SENSOR_SERVICE);


        Sensor s = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE); // barometer
        mSensorManager.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL); // in background
    }

    private void unregisterSensorListeners() {
        mSensorManager = (SensorManager) mContext.getSystemService(mContext.SENSOR_SERVICE);

        Sensor s = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

        mSensorManager.unregisterListener(this, s);

    }


    @Override
    public void addObserver(AltitudeObserver observer) {
        if (mObservers.size() == 0) {
            registerSensorListeners();
        }
        mObservers.add(observer);

    }

    @Override
    public void removeObserver(AltitudeObserver observer) {
        mObservers.remove(observer);
        if (mObservers.size() == 0) {
            unregisterSensorListeners();
        }
    }

    public void notifyObservers(float altitude) {
        for (AltitudeObserver o : mObservers) {
            o.altitude(altitude);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_PRESSURE:
                // get height based on pressure and reference value
                float pressure = event.values[0];
                float altitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure);
                // Log.d(TAG, "Altitude: " + altitude);

                if (Math.abs(mLastAltitude - altitude) >= 1) {
                    // notify only when there is at least one meter change
                    notifyObservers(altitude);
                    mLastAltitude = altitude;
                }

                break;
            default:
                Log.d(TAG, "unknown sensor type detected...");
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // do nothing...
    }
}
