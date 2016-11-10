package at.fhooe.mc.mos.logic;

import com.google.firebase.database.DatabaseReference;

import at.fhooe.mc.mos.hardware.HeartRateMonitor;
import at.fhooe.mc.mos.ui.HeartRateView;

/**
 * Created by Oliver on 10.11.2016.
 */
public class HeartRateManager implements HeartRateObserver {

    private static final String TAG = HeartRateManager.class.getSimpleName();

    private HeartRateView mView;
    private DatabaseReference mDatabaseRef;
    private HeartRateMonitor mHeartRateMonitor;


    private int mHeartRate;

    public HeartRateManager(HeartRateView view, HeartRateMonitor heartRateMonitor, DatabaseReference databaseRef) {
        mView = view;
        mDatabaseRef = databaseRef;
        mHeartRateMonitor = heartRateMonitor;
        mHeartRate = 0;

    }

    @Override
    public void heartRate(int heartRate) {
        mHeartRate = heartRate;
        mView.currentHeartRate(heartRate);
    }

    public void start() {
        // listen for steps
        mHeartRateMonitor.addObserver(this);
    }

    public void stop() {
        // stop listening
        mHeartRateMonitor.removeObserver(this);
    }

    public void reset() {
        mHeartRate = 0;
    }
}
