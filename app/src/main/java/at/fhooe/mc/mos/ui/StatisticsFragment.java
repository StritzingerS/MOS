package at.fhooe.mc.mos.ui;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import at.fhooe.mc.mos.R;
import at.fhooe.mc.mos.adapter.ExerciseAdapter;
import at.fhooe.mc.mos.model.Exercise;
import at.fhooe.mc.mos.utils.RecyclerItemClickListener;


public class StatisticsFragment extends Fragment {

    private static final String TAG = StatisticsFragment.class.getSimpleName();
    public static final String EXERCISE_KEY = "exerciseIdKey";
    private List<Exercise> mExercises;

    private View mView;
    private ProgressBar mProgressBarLoading;

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ExerciseAdapter mAdapter;

    public StatisticsFragment() {
        // Required empty public constructor
    }

    public static StatisticsFragment newInstance() {
        StatisticsFragment fragment = new StatisticsFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mExercises = new ArrayList<>();
    }

    private void showExercises(List<Exercise> mExercises) {
        mAdapter = new ExerciseAdapter(getContext(), mExercises);
        mRecyclerView.setAdapter(mAdapter);

        mRecyclerView.setVisibility(View.VISIBLE);
        mProgressBarLoading.setVisibility(View.GONE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_statistics, container, false);

        mProgressBarLoading = (ProgressBar) mView.findViewById(R.id.progressBar_statistics_loading);
        mProgressBarLoading.setVisibility(View.VISIBLE);

        mRecyclerView = (RecyclerView) mView.findViewById(R.id.rv_statistics_exercises);
        mRecyclerView.setVisibility(View.GONE);
        mLinearLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLinearLayoutManager);

        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(getContext(), new RecyclerItemClickListener.OnItemClickListener() {
                    @Override public void onItemClick(View view, int position) {
                        Exercise e = mAdapter.getItemAtPosition(position);
                        if (e != null) {
                            // start detail activity
                            Intent intent = new Intent(getContext(), ExerciseDetailActivity.class);
                            intent.putExtra(EXERCISE_KEY, e.getmKey());
                            startActivity(intent);
                        }
                    }
                })
        );

        loadExercises();

        return mView;
    }

    private void loadExercises() {
        Log.d(TAG, "start loading exercises...");

        DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Query exercisesQuery = mDatabase.child("users").child(uid).child("exercise")
                .orderByChild("mStartTime");

        exercisesQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, dataSnapshot.getValue().toString());
                for (DataSnapshot exerciseSnapshot : dataSnapshot.getChildren()) {
                    Log.d(TAG, exerciseSnapshot.getValue().toString());
                    Exercise e = exerciseSnapshot.getValue(Exercise.class);
                    e.setmKey(exerciseSnapshot.getKey());
                    mExercises.add(0, e); // add at beginning
                }

                showExercises(mExercises);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "loadExercises:onCancelled", databaseError.toException());
                Toast.makeText(getContext(), "Error getting exercises", Toast.LENGTH_SHORT).show();
            }
        });

    }
}
