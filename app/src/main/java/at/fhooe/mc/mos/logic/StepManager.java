package at.fhooe.mc.mos.logic;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import at.fhooe.mc.mos.hardware.Pedometer;
import at.fhooe.mc.mos.model.Step;
import at.fhooe.mc.mos.ui.PedometerView;

/**
 * Manager which handles the current steps taken and all related step statistics.
 * Connects to pedometer for step counting, and database for online saving.
 * Informs view whenever some values change.
 */
public class StepManager implements PedometerObserver {

    private PedometerView mView;
    private Pedometer mPedometer;
    private DatabaseReference mDatabaseRef;

    private int mStepCount;

    public StepManager(PedometerView view, Pedometer pedometer, DatabaseReference databaseRef) {
        mView = view;
        mPedometer = pedometer;
        mDatabaseRef = databaseRef;

        mStepCount = 0;
    }


    @Override
    public void stepDetected() {
        mView.currentSteps(++mStepCount);
    }

    public void startCounting() {
        // listen for steps
        mPedometer.addObserver(this);
    }

    public void stopCounter() {
        // stop listening
        mPedometer.removeObserver(this);
    }

    public void resetCounter() {
        mStepCount = 0;
    }

    public void saveData() {
        Step step = new Step();
        step.setCount(mStepCount);

        // push new value
        mDatabaseRef.push().setValue(step, new DatabaseReference.CompletionListener() {
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
}
