package at.fhooe.mc.mos.logic;

import android.content.Context;

import at.fhooe.mc.mos.hardware.AltitudeMonitor;
import at.fhooe.mc.mos.ui.AltitudeView;

/**
 * Manager for altitude measurements.
 */
public class AltitudeManager implements AltitudeObserver {

    private static final String TAG = HeartRateManager.class.getSimpleName();

    private AltitudeView mView;
    // private DatabaseReference mDatabaseRef;
    private AltitudeMonitor mMonitor;
    private Context mContext;

    private float mAltitude;

    public AltitudeManager(AltitudeView view, Context context, AltitudeMonitor altitudeMonitor/*, DatabaseReference databaseRef*/) {
        mView = view;
        mMonitor = altitudeMonitor;
        mContext = context;

        mAltitude = 0;
    }

    public void start() {
        // listen
        mMonitor.addObserver(this);
    }

    public void stop() {
        // stop listening
        mMonitor.removeObserver(this);
    }

    @Override
    public void altitude(float altitude) {
        mAltitude = altitude;
        mView.currentAltitude(altitude);
    }
}
