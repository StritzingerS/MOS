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
    private Context mContext;

    private double mCalories;   // in kilo calories
    private long mLastHeartBeat;
    private int mHeartRate;
    private double mAvgHeartRate;
    private int mAvgHRCounter;
    private int mHRMax;
    private int mHRMin;

    private int mGender;    // 0 = females, 1 = males
    private int mWeight;    // Weight in kg
    private int mAge;       //  Age in years
    private double mVO2max;
    private int mMaximumHR;


    public HeartRateManager(HeartRateView view, Context _context, HeartRateMonitor heartRateMonitor, DatabaseReference databaseRef) {
        mView = view;
        mDatabaseRef = databaseRef;
        mHeartRateMonitor = heartRateMonitor;
        mContext=_context;
        mCalories = 0;
        mLastHeartBeat = 0;
        mHeartRate = 0;
        mAvgHeartRate = 0;
        mAvgHRCounter = 0;
        mHRMax = Integer.MIN_VALUE;
        mHRMin = Integer.MAX_VALUE;

        //load settings for VO2max calculation
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
        int PAR = Integer.parseInt(sharedPreferences.getString("keyPar", "5"));
        mVO2max = (0.133 * mAge) - (0.005 * mAge * mAge) + (11.403 * mGender) + (1.463 * PAR) +
                (9.17 * height / 100) - (0.254 * mWeight) + (34.143);

        mMaximumHR = Integer.parseInt(sharedPreferences.getString("keyHrMax", "0"));
        Log.i(TAG, "max hr 1: " + mMaximumHR);
        if(mMaximumHR==0){
            mMaximumHR= (int)(208-(0.7*(double)mAge));
        }

        Log.i(TAG, "max hr 2: " + mMaximumHR);

    }

    @Override
    public void heartRate(int heartRate) {
        Log.i(TAG, "heartrate: " + heartRate);
        mHeartRate = heartRate;

        mAvgHeartRate = ((heartRate-mAvgHeartRate)/(++mAvgHRCounter))+mAvgHeartRate;

        if (heartRate > mHRMax) {
            mHRMax = heartRate;
        }

        if(heartRate < mHRMin){
            mHRMin = heartRate;
        }

        if (mLastHeartBeat != 0) {
            calculateCalories();
        } else {
            mLastHeartBeat = System.currentTimeMillis();
        }


        mView.currentHeartRate(heartRate);
        mView.currentCalories((int) mCalories);
        mView.currentAvgHeartRate((int)mAvgHeartRate);
        mView.currentHrMaxPercentage((heartRate*100/mMaximumHR));
    }

    private void calculateCalories() {
        long currentHeartbeat = System.currentTimeMillis();
        double timeDiffSec = (currentHeartbeat - mLastHeartBeat) / 1000.0;
        mLastHeartBeat = currentHeartbeat;

        double kJoulesPerMin = (-59.3954 + mGender * (-36.3781 + 0.271 * mAge + 0.394 * mWeight + 0.404 *
                mVO2max + 0.634 * mHeartRate) + (1 - mGender) * (0.274 * mAge + 0.103 * mWeight +
                0.38 * mVO2max + 0.45 * mHeartRate));

        Log.i(TAG, "kJoules: " + kJoulesPerMin);
        mCalories = mCalories + (kJoulesPerMin / 4.168 / 60 * timeDiffSec);
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
        mLastHeartBeat = 0;
        mAvgHeartRate = 0;
        mAvgHRCounter = 0;
        mHRMax = Integer.MIN_VALUE;
        mHRMin = Integer.MAX_VALUE;
    }

    public double getCalories() {
        return mCalories;
    }

    public double getAvgHeartRate(){ return mAvgHeartRate; }

    public double getTrimp(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        int hrRest = Integer.parseInt(sharedPreferences.getString("keyHrRest", "60"));
        return ((mAvgHeartRate-hrRest)/(mMaximumHR-hrRest));
    }

    public int getmHRMax(){
        return mHRMax;
    }

    public int getmHRMin(){
        return mHRMin;
    }
}
