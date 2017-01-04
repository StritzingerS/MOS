package at.fhooe.mc.mos.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.List;

import at.fhooe.mc.mos.R;
import at.fhooe.mc.mos.model.Exercise;
import at.fhooe.mc.mos.utils.MillisHelper;

/**
 * Adapter for statistics recyclerView
 */
public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.CustomViewHolder> {
    private List<Exercise> mExercises;
    private Context mContext;

    public ExerciseAdapter(Context context, List<Exercise> exercises) {
        this.mExercises = exercises;
        this.mContext = context;

    }

    @Override
    public CustomViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_item_exercise, viewGroup, false);
        CustomViewHolder viewHolder = new CustomViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(CustomViewHolder customViewHolder, int i) {
        Exercise exercise = mExercises.get(i);

        //Setting text view title
        customViewHolder.mTvStartTime.setText(MillisHelper.millisToLocaleString(exercise.getmStartTime()));
        customViewHolder.mTvDistance.setText(String.valueOf(exercise.getmStepCount()));
        customViewHolder.mTvDuration.setText(MillisHelper.millisToDuration(exercise.getmDuration()));
    }

    @Override
    public int getItemCount() {
        return (null != mExercises ? mExercises.size() : 0);
    }


    class CustomViewHolder extends RecyclerView.ViewHolder {
        protected TextView mTvStartTime;
        protected TextView mTvDistance;
        protected TextView mTvDuration;

        public CustomViewHolder(View view) {
            super(view);
            this.mTvStartTime = (TextView) view.findViewById(R.id.tv_item_exercise_starttime);
            this.mTvDistance = (TextView) view.findViewById(R.id.tv_item_exercise_distance);
            this.mTvDuration = (TextView) view.findViewById(R.id.tv_item_exercise_duration);
        }
    }
}
