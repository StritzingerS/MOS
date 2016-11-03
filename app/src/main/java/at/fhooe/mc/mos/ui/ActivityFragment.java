package at.fhooe.mc.mos.ui;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import at.fhooe.mc.mos.R;
import at.fhooe.mc.mos.logic.Pedometer;
import at.fhooe.mc.mos.logic.PedometerObserver;
import at.fhooe.mc.mos.model.Step;
import at.grabner.circleprogress.CircleProgressView;
import at.grabner.circleprogress.TextMode;


public class ActivityFragment extends Fragment implements PedometerObserver, View.OnClickListener {

    private static final String TAG = ActivityFragment.class.getSimpleName();

    private View mView;
    private Button mBtnStart;
    private Button mBtnStop;
    private DatabaseReference mFirebaseDatabaseReference;

    private CircleProgressView mCircleView;
    private Pedometer mPedometer;
    private int mCurrentSteps;


    public ActivityFragment() {
        // Required empty public constructor
    }

    public static ActivityFragment newInstance() {
        ActivityFragment fragment = new ActivityFragment();
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_activity, container, false);

        // Connection to Firebase
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users").child(uid).child("steps");

        // Circular View
        int maxSteps = getMaxSteps();

        mCircleView = (CircleProgressView) mView.findViewById(R.id.circleView);
        mCircleView.setMaxValue(maxSteps);

        mCircleView.setUnit("/ " + maxSteps);
        mCircleView.setUnitVisible(true);

        mCircleView.setTextMode(TextMode.VALUE); // Set text mode to text to show text
        mCircleView.setValueAnimated(0);

        mCircleView.setOnProgressChangedListener(new CircleProgressView.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(float value) {
                Log.d(TAG, "Progress Changed: " + value);
            }
        });

        // Buttons
        mBtnStart = (Button) mView.findViewById(R.id.btn_activity_start);
        mBtnStart.setOnClickListener(this);

        mBtnStop = (Button) mView.findViewById(R.id.btn_activity_stop);
        mBtnStop.setOnClickListener(this);

        return mView;
    }

    private int getMaxSteps() {

        SharedPreferences prefs = getContext().getSharedPreferences(SettingsFragment.MY_PREFS, getContext().MODE_PRIVATE);
        int maxSteps = prefs.getInt(SettingsFragment.KEY_MAX_STEPS, 0);
        if (maxSteps == 0) {
            maxSteps = 100;
        }
        return maxSteps;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mPedometer != null) {
            mPedometer.removeObserver(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mPedometer != null) {
            mPedometer.addObserver(this);
        }
    }

    private void setUpPedometer(Context context) {
        mPedometer = Pedometer.getInstance(context);
        mPedometer.addObserver(this);
    }


    @Override
    public void stepDetected() {
        mCurrentSteps++;
        mCircleView.setValueAnimated(mCurrentSteps);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btn_activity_start:
                setUpPedometer(v.getContext());
                mBtnStart.setEnabled(false);
                mBtnStop.setEnabled(true);

                break;
            case R.id.btn_activity_stop:
                mBtnStart.setEnabled(false);
                mBtnStop.setEnabled(false);
                mPedometer.removeObserver(this);

                saveData();

                break;
            default:
                Log.d(TAG, "unknown onclick id encountered...");
        }
    }

    /**
     * Saves data online.
     */
    private void saveData() {

        Step step = new Step();
        step.setCount(mCurrentSteps);

        // push new value
        mFirebaseDatabaseReference.push().setValue(step, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError == null) {
                    Toast.makeText(getContext(), "Data saved online", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), "Error saving data: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                }

                // enable button
                mBtnStart.setEnabled(true);
                mBtnStop.setEnabled(false);

                // reset
                mCurrentSteps = 0;
                mCircleView.setValueAnimated(0);
            }
        });
    }
}
