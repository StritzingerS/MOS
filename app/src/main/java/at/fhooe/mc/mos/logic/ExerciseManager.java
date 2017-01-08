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
    private AltitudeManager mAltitudeManager;
    private int mWeight;

    private int mStepGoal;
    private int mStepCount;
    private long mStartTime;
    private long mStopTime;
    private double mStrideLength;       //in meters
    private double mRunningDistance;    //in meters
    private double mLastDistance;       //in meters (used for altitude calorie calculation)
    private double mEquivalentDistance; //in meters
    private double mCalories;           // in kilo calories
    private double mCaloriesPerStep;    // in kilo calories
    private double mCaloriesWithAltitude; // in kilo calories
    private long mLastStepTime;
    private float mLastAltitude;
    private float mPace;                // in sec/km
    private float mEquivalentPace;      // in sec/km


    public ExerciseManager(PedometerView view, Context _context, Pedometer pedometer, DatabaseReference databaseRef) {
        mView = view;
        mPedometer = pedometer;
        mDatabaseRef = databaseRef;

        reset();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(_context);
        mStepGoal = Integer.parseInt(sharedPreferences.getString("keySteps", "1000"));

        //load settings for VO2max calculation
        mWeight = Integer.parseInt(sharedPreferences.getString("keyWeight", "80"));
        int age = Integer.parseInt(sharedPreferences.getString("keyAge", "20"));
        int height = Integer.parseInt(sharedPreferences.getString("keyHeight", "180"));
        String gender = sharedPreferences.getString("keyGender", "Male");
        if (gender.compareTo("Female") == 0) {
            mStrideLength = -0.001*age + 1.058*height/100-0.002*mWeight-0.129;
        } else {
            mStrideLength = -0.002*age + 0.76*height/100-0.001*mWeight+0.327;
        }
        mCaloriesPerStep = mWeight*9.81*0.03/0.60/4.1868/1000 ; //weight * gravity * vertical oszialltion / vertical to horzontal movment ratio
    }

    private float getSlopeEnergyCost(float gradient){
        if(Math.abs(gradient) < 0.45f) {
            return 40.3833f * (float) Math.pow(gradient, 2) + 16.675f * gradient + 3.69867f;
        }
        else{
            return 4.1f;
        }
    }

    @Override
    public void stepDetected() {
        mView.currentSteps(++mStepCount);
        long currentTime = System.currentTimeMillis();

        //Calories
        if(mLastStepTime!= 0){
            mCalories+= mCaloriesPerStep + (currentTime-mLastStepTime)/1000.0/60.0/60.0*mWeight; //add CaloriesPerStep BMR
            if(mHeartRateManager == null) {
                mView.currentCalories((int) mCalories);
            }
        }else {
            mLastStepTime = currentTime;
        }

        //use only every second step for a full stride
        if( (mStepCount & 1) == 0){

            //Distance and Pace
            mRunningDistance+=mStrideLength;
            mPace= ((currentTime-mStartTime)/1000)/(float)(mRunningDistance/1000);
            mView.currentDistance(((int)(mRunningDistance/10))/100.0); //return in kilometers
            mView.currentPace(mPace);


            //Equivalent Distance and Pace
            if(mAltitudeManager != null ){
                float currentAltitude = mAltitudeManager.getAltitude();
                if(mLastAltitude != 0.0f || mLastDistance != 0.0f){
                    float gradient = (currentAltitude-mLastAltitude)/(float)mStrideLength ;
                    mCaloriesWithAltitude += mWeight * getSlopeEnergyCost(gradient)*(mRunningDistance-mLastDistance)/4186.8;
                    mEquivalentDistance=mCaloriesWithAltitude*4.1868/(mWeight*4.1)*1000;
                    mEquivalentPace=((currentTime-mStartTime)/1000)/(float)(mEquivalentDistance/1000);
                    mView.currentEquivalentDistance(((int)(mEquivalentDistance/10))/100.0);
                    mView.currentEquivalentPace(mEquivalentPace);
                }
                mLastAltitude = currentAltitude;
                mLastDistance = mRunningDistance;
            }
        }
        mLastStepTime=currentTime;
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
        mLastDistance=0;
        mEquivalentDistance=0.0;
        mCalories =0.0;
        mCaloriesWithAltitude=0.0;
        mLastStepTime=0;
        mLastAltitude=0;
        mPace=0;
        mEquivalentPace=0;
    }

    public void saveData() {
        Exercise exercise = new Exercise();
        exercise.setmStepGoal(mStepGoal);
        exercise.setmStepCount(mStepCount);
        exercise.setmDuration(mStopTime-mStartTime);
        exercise.setmStartTime(mStartTime);
        exercise.setmRunningDistance(mRunningDistance);
        exercise.setmPace(mPace);

        if (mAltitudeManager != null) {
            exercise.setmEquivalentDistance(mEquivalentDistance);
            exercise.setmEquivalentPace(mEquivalentPace);
            exercise.setmCalorieCount((int)mCalories);
            exercise.setmCaloriesWithAltitude(mCaloriesWithAltitude);
        }
        if(mHeartRateManager != null){
            exercise.setmCalorieCount((int)mHeartRateManager.getCalories());
            exercise.setmAvgHeartRate((int)mHeartRateManager.getAvgHeartRate());
            exercise.setmMaxHeartRate(mHeartRateManager.getmHRMax());
            exercise.setmMinHeartRate(mHeartRateManager.getmHRMin());
            exercise.setmTrimp(mHeartRateManager.getTrimp()*(mStopTime-mStartTime)/1000.0/60.0);
        }
        if (mHeartRateManager == null && mAltitudeManager == null){
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
    public void setAltitudeManager(AltitudeManager altitudeManager){
        mAltitudeManager = altitudeManager;
    }
}
