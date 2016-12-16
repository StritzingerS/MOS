package at.fhooe.mc.mos.logic;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import at.fhooe.mc.mos.hardware.Pedometer;
import at.fhooe.mc.mos.model.Exercise;
import at.fhooe.mc.mos.ui.PedometerView;

import static android.content.ContentValues.TAG;

/**
 * Manager which handles the current steps taken and all related step statistics.
 * Connects to pedometer for step counting, and database for online saving.
 * Informs view whenever some values change.
 */
public class ExerciseManager implements PedometerObserver {

    private PedometerView mView;
    private Pedometer mPedometer;
    private DatabaseReference mDatabaseRef;
    private HeartRateManager mHeartRateManager;
    private int mWeight;

    private int mStepGoal;
    private int mStepCount;
    private long mStartTime;
    private long mStopTime;
    private double mStrideLength;       //in meters
    private double mRunningDistance;
    private double mCalories;           // in kilo calories
    private double mCaloriesPerStep;    // in kilo calories
    private long mLastStepTime;

    public ExerciseManager(PedometerView view, Context _context, Pedometer pedometer, DatabaseReference databaseRef) {
        mView = view;
        mPedometer = pedometer;
        mDatabaseRef = databaseRef;

        mStepCount = 0;
        mRunningDistance=0.0;
        mLastStepTime=0;
        mCalories =0.0;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_context);
        mStepGoal = Integer.parseInt(sharedPreferences.getString("keyMaxSteps", "100"));

        //load settings for VO2max calculation
        int weight = Integer.parseInt(sharedPreferences.getString("keyWeight", "80"));
        int age = Integer.parseInt(sharedPreferences.getString("keyAge", "20"));
        int mWeight = Integer.parseInt(sharedPreferences.getString("keyHeight", "180"));
        String gender = sharedPreferences.getString("keyGender", "Male");
        if (gender.compareTo("Female") == 0) {
            mStrideLength = -0.001*age + 1.058*mWeight/100-0.002*weight-0.129;
        } else {
            mStrideLength = -0.002*age + 0.76*mWeight/100-0.001*weight+0.327;
        }
        mCaloriesPerStep = weight*9.81*0.03/0.60/4.1868/1000 ; //weight * gravity * vertical oszialltion / vertical to horzontal movment ratio
    }


    @Override
    public void stepDetected() {
        mView.currentSteps(++mStepCount);
        if( (mStepCount & 1) == 0){
            mRunningDistance+=mStrideLength;
            mView.currentDistance(((int)(mRunningDistance/10))/100.0); //return in kilometers
        }

        if(mLastStepTime!= 0){
            long currentTime = System.currentTimeMillis();
            mCalories+= mCaloriesPerStep + (currentTime-mLastStepTime)/1000.0/60.0/60.0*mWeight;
            Log.i(TAG, "Step Calories: " + mCalories);
            if(mHeartRateManager == null) {
                mView.currentCalories((int) mCalories);
            }
        }else {
            mLastStepTime = System.currentTimeMillis();
        }
    }

    public void startCounting() {
        // listen for steps
        mPedometer.addObserver(this);
        mStartTime = System.currentTimeMillis();
        if(mHeartRateManager != null) {
            mHeartRateManager.setCountCalories(true);
        }
    }

    public void stopCounter() {
        // stop listening
        mPedometer.removeObserver(this);
        mStopTime = System.currentTimeMillis();
        if(mHeartRateManager != null) {
            mHeartRateManager.setCountCalories(false);
        }
    }

    public void reset() {
        mStepCount = 0;
        mRunningDistance=0.0;
        mLastStepTime=0;
    }

    public void saveData() {
        Exercise exercise = new Exercise();
        exercise.setmStepGoal(mStepGoal);
        exercise.setmStepCount(mStepCount);
        exercise.setmDuration(mStopTime-mStartTime);
        exercise.setmStartTime(mStartTime);

        if(mHeartRateManager != null){
            exercise.setmCalorieCount((int)mHeartRateManager.getCalories());
            exercise.setmAvgHeartRate((int)mHeartRateManager.getAvgHeartRate());
            exercise.setmMaxHeartRate(mHeartRateManager.getmHRMax());
            exercise.setmMinHeartRate(mHeartRateManager.getmHRMin());
            exercise.setmTrimp(mHeartRateManager.getTrimp()*(mStopTime-mStartTime)/1000.0/60.0);
        }else{
            exercise.setmCalorieCount((int)mCalories);
        }

        // push new value
        mDatabaseRef.push().setValue(exercise, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError == null) {
                    mView.dataSaved(true);
                } else {
                    mView.dataSaved(false);
                }
            }
        });
    }

    public void setHeartRateManager(HeartRateManager heartRateManager){
        mHeartRateManager = heartRateManager;
    }
}
