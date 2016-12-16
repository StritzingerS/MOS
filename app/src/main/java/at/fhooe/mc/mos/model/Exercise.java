package at.fhooe.mc.mos.model;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple model of a step for testing purposes.
 */
public class Exercise {

    private int mStepGoal;
    private int mStepCount;
    private int mCalorieCount;
    private int mAvgHeartRate;
    private int mMaxHeartRate;
    private int mMinHeartRate;
    private long mDuration;
    private long mStartTime;
    private double mTrimp;

    public Exercise() {
        // Default constructor required for calls to DataSnapshot.getValue(ListItem.class)
    }

    public int getmStepGoal() { return mStepGoal; }
    public void setmStepGoal(int mStepGoal) { this.mStepGoal = mStepGoal; }

    public int getmStepCount() {
        return mStepCount;
    }
    public void setmStepCount(int mStepCount) {
        this.mStepCount = mStepCount;
    }

    public int getmCalorieCount() {  return mCalorieCount;   }
    public void setmCalorieCount(int mCalorieCount) { this.mCalorieCount = mCalorieCount; }

    public int getmAvgHeartRate() {        return mAvgHeartRate;    }
    public void setmAvgHeartRate(int mAvgHeartRate) { this.mAvgHeartRate = mAvgHeartRate; }

    public int getmMaxHeartRate() {        return mMaxHeartRate;    }
    public void setmMaxHeartRate(int mMaxHeartRate) {        this.mMaxHeartRate = mMaxHeartRate;    }

    public int getmMinHeartRate() {        return mMinHeartRate;    }
    public void setmMinHeartRate(int mMinHeartRate) {        this.mMinHeartRate = mMinHeartRate;    }

    public long getmDuration() {        return mDuration;    }
    public void setmDuration(long mTimeInMilli) { this.mDuration = mTimeInMilli; }

    public long getmStartTime() { return mStartTime; }
    public void setmStartTime(long mStartTime) { this.mStartTime = mStartTime; }

    public double getmTrimp() {        return mTrimp;    }
    public void setmTrimp(double mTrimp) {        this.mTrimp = mTrimp;    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("mStepCount", mStepCount);
        return result;
    }
}
