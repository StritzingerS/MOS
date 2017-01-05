package at.fhooe.mc.mos.ui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import at.fhooe.mc.mos.R;
import at.fhooe.mc.mos.model.Exercise;
import at.fhooe.mc.mos.utils.TimeHelper;

public class ExerciseDetailActivity extends AppCompatActivity {

    private static final String TAG = ExerciseDetailActivity.class.getSimpleName();

    private TextView mTvDate;
    private TextView mTvDuration;
    private TextView mTvSteps;
    private TextView mTvGoal;
    private TextView mTvCalories;
    private TextView mTvHeartRate;
    private TextView mTvTRIMP;
    private TextView mTvPace;
    private TextView mTvDistance;

    private View mContent;
    private ProgressBar mProgressBarLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_detail);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_exercisedetail);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.activity_exercisedetails_title);

        mContent = findViewById(R.id.view_exercisedetails_content);
        mContent.setVisibility(View.GONE);
        mProgressBarLoading = (ProgressBar) findViewById(R.id.progressBar_exercisedetails_loading);
        mProgressBarLoading.setVisibility(View.VISIBLE);

        mTvDate = (TextView) findViewById(R.id.tv_exercisedetail_date);
        mTvDuration = (TextView) findViewById(R.id.tv_exercisedetail_duration);
        mTvSteps = (TextView) findViewById(R.id.tv_exercisedetail_steps);
        mTvGoal = (TextView) findViewById(R.id.tv_exercisedetail_goal);
        mTvCalories = (TextView) findViewById(R.id.tv_exercisedetail_calories);
        mTvHeartRate = (TextView) findViewById(R.id.tv_exercisedetail_heartRate);
        mTvTRIMP = (TextView) findViewById(R.id.tv_exercisedetail_trimp);
        mTvPace = (TextView) findViewById(R.id.tv_exercisedetail_pace);
        mTvDistance = (TextView) findViewById(R.id.tv_exercisedetail_distance);

        Intent intent = getIntent();
        String exerciseKey = intent.getStringExtra(StatisticsFragment.EXERCISE_KEY);
        if (exerciseKey != null && exerciseKey != "") {
            loadExercises(exerciseKey);
        } else {
            Log.d(TAG, "error getting exercise id...");
        }
    }


    private void loadExercises(String exerciseKey) {
        Log.d(TAG, "start loading exercise...");

        DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Query exerciseQuery = mDatabase.child("users").child(uid).child("exercise").child(exerciseKey);

        exerciseQuery.addListenerForSingleValueEvent(new ValueEventListener() {


            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, dataSnapshot.getValue().toString());
                Exercise e = dataSnapshot.getValue(Exercise.class);
                showExercise(e);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "loadExercises:onCancelled", databaseError.toException());
                Toast.makeText(ExerciseDetailActivity.this, "Error getting exercise", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

    }

    private void showExercise(Exercise e) {
        mTvDate.setText(TimeHelper.millisToLocaleString(e.getmStartTime()));
        mTvDuration.setText(TimeHelper.millisToDuration(e.getmDuration()));
        mTvSteps.setText(String.valueOf(e.getmStepCount()));
        mTvGoal.setText(String.valueOf(e.getmStepGoal()));
        mTvCalories.setText(String.valueOf(e.getmCalorieCount())+ "/" + String.valueOf((int)e.getmCaloriesWithAltitude()));
        mTvHeartRate.setText(String.valueOf(e.getmMinHeartRate()) + "/" + String.valueOf(e.getmMaxHeartRate()) + "/" + String.valueOf(e.getmAvgHeartRate()));
        mTvTRIMP.setText(String.valueOf(((int)(e.getmTrimp()*100))/100.0));
        mTvPace.setText(TimeHelper.secondsToDuration((long)e.getmPace()) + "/" + TimeHelper.secondsToDuration((long)e.getmEquivalentPace()));
        mTvDistance.setText(String.valueOf((int)e.getmRunningDistance()) + "/" + String.valueOf((int)e.getmEquivalentDistance()));

        mProgressBarLoading.setVisibility(View.GONE);
        mContent.setVisibility(View.VISIBLE);
    }
}
