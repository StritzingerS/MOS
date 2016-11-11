package at.fhooe.mc.mos.logic;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

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
    private double mCalories;

    private int mGender;    // 0 = females, 1 = males
    private int mWeight;    // Weight in kg
    private int mAge;       //  Age in years
    private double mVO2max;

    public HeartRateManager(HeartRateView view, Context _context, HeartRateMonitor heartRateMonitor, DatabaseReference databaseRef) {
        mView = view;

        mDatabaseRef = databaseRef;
        mHeartRateMonitor = heartRateMonitor;
        mHeartRate = 0;
        mCalories = 0;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_context);
        mWeight = Integer.parseInt(sharedPreferences.getString("keyWeight", "80"));
        mAge = Integer.parseInt(sharedPreferences.getString("keyAge", "20"));
        String gender = sharedPreferences.getString("keyGender", "Male");
        if (gender.compareTo("Female") == 0) {
            mGender = 0;
        } else {
            mGender = 1;
        }
        int height = Integer.parseInt(sharedPreferences.getString("keyHeight", "180"));
        int PAR = Integer.parseInt(sharedPreferences.getString("keyPAR", "5"));

        mVO2max = (0.133 * mAge) - (0.005 * mAge * mAge) + (11.403 * mGender) + (1.463 * PAR) +
                (9.17 * height) - (0.254 * mWeight) + (34.143);

        Log.i(TAG, "Weight: " + mWeight + " Gender: " + mGender + " Age: " + mAge);

    }

    @Override
    public void heartRate(int heartRate) {
        mHeartRate = heartRate;
        mView.currentHeartRate(heartRate);

        mView.currentCalories((int) mCalories);
    }

    private void calculateCalories() {
        mCalories = (-59.3954 + mGender * (-36.3781 + 0.271 * mAge + 0.394 * mWeight + 0.404 *
                mVO2max + 0.634 * mHeartRate) + (1 - mGender) * (0.274 * mAge + 0.103 * mWeight +
                0.38 * mVO2max + 0.45 * mHeartRate));

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
        mCalories = 0;
    }
}
