package at.fhooe.mc.mos.ui;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import at.fhooe.mc.mos.R;
import at.fhooe.mc.mos.hardware.AndroidPedometer;
import at.fhooe.mc.mos.logic.StepManager;
import at.grabner.circleprogress.CircleProgressView;
import at.grabner.circleprogress.TextMode;

/**
 * Fragment for starting and stopping a activity session.
 */
public class ActivityFragment extends Fragment implements PedometerView, View.OnClickListener {

    private static final String TAG = ActivityFragment.class.getSimpleName();

    private View mView;
    private Button mBtnStart;
    private Button mBtnStop;


    private CircleProgressView mCircleView;
    private StepManager mStepManager;

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

        // Connection to Firebase
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference().child("users").child(uid).child("steps");
        // needs application context to prevent memory leaks
        mStepManager = new StepManager(this, AndroidPedometer.getInstance(getActivity().getApplicationContext()), databaseRef);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_activity, container, false);


        // Circular View
        int maxSteps = getMaxSteps();

        mCircleView = (CircleProgressView) mView.findViewById(R.id.circleView);

        // gradient
        mCircleView.setBarColor(getResources().getColor(R.color.red), getResources().getColor(R.color.green));

        mCircleView.setMaxValue(maxSteps);

        mCircleView.setUnit("/ " + maxSteps);
        mCircleView.setUnitVisible(true);

        mCircleView.setTextMode(TextMode.VALUE); // Set text mode to text to show text
        mCircleView.setValueAnimated(0);

        /*
        mCircleView.setOnProgressChangedListener(new CircleProgressView.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(float value) {
                Log.d(TAG, "Progress Changed: " + value);
            }
        });
        */

        // Buttons
        mBtnStart = (Button) mView.findViewById(R.id.btn_activity_start);
        mBtnStart.setOnClickListener(this);

        mBtnStop = (Button) mView.findViewById(R.id.btn_activity_stop);
        mBtnStop.setOnClickListener(this);

        return mView;
    }

    private int getMaxSteps() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getContext());

        // SharedPreferences prefs = getContext().getSharedPreferences(SettingsActivity.MY_PREFS, getContext().MODE_PRIVATE);
        int maxSteps = Integer.parseInt(prefs.getString("keyMaxSteps", "0"));
        if (maxSteps == 0) {
            maxSteps = 100;
        }
        return maxSteps;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btn_activity_start:
                mStepManager.startCounting();
                mBtnStart.setEnabled(false);
                mBtnStop.setEnabled(true);

                break;
            case R.id.btn_activity_stop:
                mBtnStart.setEnabled(false);
                mBtnStop.setEnabled(false);

                mStepManager.stopCounter();

                // saving online
                mStepManager.saveData();

                break;
            default:
                Log.d(TAG, "unknown onclick id encountered...");
        }
    }


    @Override
    public void currentSteps(int currentSteps) {
        mCircleView.setValueAnimated(currentSteps);
    }

    @Override
    public void dataSaved(boolean success) {
        if (success) {
            Toast.makeText(getContext(), "Data saved online", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), "Error saving data", Toast.LENGTH_LONG).show();
        }

        // TODO: handle errors
        // treat as success for now...
        mStepManager.resetCounter();

        // enable button
        mBtnStart.setEnabled(true);
        mBtnStop.setEnabled(false);

        // reset view
        mCircleView.setValueAnimated(0);
    }
}
