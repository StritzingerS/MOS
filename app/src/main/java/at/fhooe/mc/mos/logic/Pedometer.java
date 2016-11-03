package at.fhooe.mc.mos.logic;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom pedometer which informs the observers with the current steps taken.
 * Use 'getInstance(...)' to access pedometer.
 */
public class Pedometer implements SensorEventListener {

    private static Pedometer INSTANCE;

    private List<PedometerObserver> mObservers;
    private Context mContext;

    SensorManager mSensorManager;

    private double[][] mAccelArray = new double[3][50];
    private int mAccelArrayCounter = 0;  //current Index of accel array
    private int mCurrAxis = 0;           //the axis of the accel array, that will be used for calculation

    private double mThresholdLevel = 0.0;
    private int mStepCount = 0;

    private int mApiStepCount = 0;

    long mLastStep = System.currentTimeMillis();
    long mLastSensorUpdate = System.currentTimeMillis();

    public static Pedometer getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new Pedometer(context);
        }
        return INSTANCE;
    }

    private Pedometer(Context context) {
        mObservers = new ArrayList<>();
        mContext = context;

        registerSensorListeners();
    }

    private void registerSensorListeners() {
        mSensorManager = (SensorManager) mContext.getSystemService(mContext.SENSOR_SERVICE);

        Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor stepDetector = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_NORMAL);

        mApiStepCount = 0;
        mLastStep = System.currentTimeMillis();
        mLastSensorUpdate = System.currentTimeMillis();
    }

    private void unregisterSensorListeners() {
        mSensorManager = (SensorManager) mContext.getSystemService(mContext.SENSOR_SERVICE);

        Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor stepDetector = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        mSensorManager.unregisterListener(this, accelerometer);
        mSensorManager.unregisterListener(this, stepDetector);
    }

    public void addObserver(PedometerObserver observer) {
        mObservers.add(observer);
    }

    public void removeObserver(PedometerObserver observer) {
        mObservers.remove(observer);
        if (mObservers.isEmpty()) {
            // remove all listeners
            // unregisterSensorListeners();
        }
    }

    public void notifyObservers() {
        for (PedometerObserver o : mObservers) {
            o.stepDetected();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent _event) {
        switch (_event.sensor.getType()) {
            case Sensor.TYPE_STEP_DETECTOR:
                // only for checks
                mApiStepCount++;

                break;
            case Sensor.TYPE_ACCELEROMETER:
                //check if enough time has passed since the last sensor reading (android "randomly" gets sensor updates)
                if ((System.currentTimeMillis() - mLastSensorUpdate) >= 20) {

                    //get acceleration value
                    mAccelArray[0][mAccelArrayCounter] = _event.values[0];
                    mAccelArray[1][mAccelArrayCounter] = _event.values[1];
                    mAccelArray[2][mAccelArrayCounter] = _event.values[2];

                    //get previous acceleration value index
                    int previousStep = mAccelArrayCounter - 1;
                    if (mAccelArrayCounter == 0) {
                        previousStep = mAccelArray.length - 1;
                    }

                    //check if acceleration resembles a step
                    // 1. acceleration is below the threshold Level
                    // 2. previous value was over of the threshold Level (and thus the acceleration is on a negative slope)
                    // 3. the peaks used in the threshold calculation need to have been a certain minimum
                    if (mThresholdLevel > mAccelArray[mCurrAxis][mAccelArrayCounter] &&
                            mAccelArray[mCurrAxis][previousStep] >= mThresholdLevel &&
                            (getMax(mAccelArray[mCurrAxis]) - getMin(mAccelArray[mCurrAxis])) > 4.0) {

                        long timeFromLastStep = System.currentTimeMillis() - mLastStep;
                        mLastStep = System.currentTimeMillis();
                        if (timeFromLastStep > 200 && timeFromLastStep < 2000) {
                            mStepCount++;
                            //stepDifference= removeCommas(mAccelArray[mCurrAxis][previousStep] - mAccelArray[mCurrAxis][mAccelArrayCounter]);

                            // notify observers
                            notifyObservers();
                        }
                    }

                    //get new acceleration array position
                    if (mAccelArrayCounter == 49) {
                        mAccelArrayCounter = 0;

                        //get which axis has the biggest amplitude
                        double diffAxis1 = (getMax(mAccelArray[0]) - getMin(mAccelArray[0]));
                        double diffAxis2 = (getMax(mAccelArray[1]) - getMin(mAccelArray[1]));
                        double diffAxis3 = (getMax(mAccelArray[2]) - getMin(mAccelArray[2]));

                        if (diffAxis1 > diffAxis2 && diffAxis1 > diffAxis3) {
                            mCurrAxis = 0;
                        } else if (diffAxis2 > diffAxis1 && diffAxis2 > diffAxis3) {
                            mCurrAxis = 1;
                        } else if (diffAxis3 > diffAxis2 && diffAxis3 > diffAxis1) {
                            mCurrAxis = 2;
                        }

                        //calculate new threshold
                        mThresholdLevel = (getMax(mAccelArray[mCurrAxis]) + getMin(mAccelArray[mCurrAxis])) / 2;
                    } else {
                        mAccelArrayCounter++;
                    }

                    mLastSensorUpdate = System.currentTimeMillis();

                }
                break;
            default:
                break;
        }
    }

    private double getMax(double[] Array) {
        double maxVal = Double.MIN_VALUE;
        for (int i = 0; i < Array.length; i++) {
            if (Array[i] > maxVal) {
                maxVal = Array[i];
            }
        }
        return maxVal;
    }

    private double getMin(double[] Array) {
        double minVal = Double.MAX_VALUE;
        for (int i = 0; i < Array.length; i++) {
            if (Array[i] < minVal) {
                minVal = Array[i];
            }
        }
        return minVal;
    }

    private double removeCommas(double value) {
        return ((int) (value * 1000.0)) / 1000.0;
    }

    @Override
    public void onAccuracyChanged(Sensor _sensor, int _accuracy) {

    }

}
