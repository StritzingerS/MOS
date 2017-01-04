package at.fhooe.mc.mos.ui;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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


public class StatisticsFragment extends Fragment {

    private static final String TAG = StatisticsFragment.class.getSimpleName();
    private List<Exercise> mExercises;

    private View mView;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_statistics, container, false);

        mRecyclerView = (RecyclerView) mView.findViewById(R.id.rv_statistics_exercises);
        mLinearLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLinearLayoutManager);

        loadExercises();

        return mView;
    }

    private void loadExercises() {
        Log.d(TAG, "start loading exercises...");

        DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Query myTopPostsQuery = mDatabase.child("users").child(uid).child("exercise")
                .orderByChild("mStartTime");

        myTopPostsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, dataSnapshot.getValue().toString());
                for (DataSnapshot exerciseSnapshot : dataSnapshot.getChildren()) {
                    Log.d(TAG, exerciseSnapshot.getValue().toString());
                    Exercise e = exerciseSnapshot.getValue(Exercise.class);
                    mExercises.add(0, e); // add at beginning
                }

                showExercises(mExercises);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "loadExercises:onCancelled", databaseError.toException());
            }
        });

    }
}
