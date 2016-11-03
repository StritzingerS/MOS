package at.fhooe.mc.mos.ui;

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

import at.fhooe.mc.mos.R;


public class SettingsFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = SettingsFragment.class.getSimpleName();

    public static final String MY_PREFS = "MyPrefs" ;
    public static final String KEY_MAX_STEPS = "KeyMaxSteps";

    private View mView;
    private Button mBtnSave;
    private EditText mEtMaxSteps;


    public SettingsFragment() {
        // Required empty public constructor
    }

    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_settings, container, false);

        mEtMaxSteps = (EditText) mView.findViewById(R.id.et_settings_max_steps);
        SharedPreferences prefs = getContext().getSharedPreferences(MY_PREFS, getContext().MODE_PRIVATE);
        int maxSteps = prefs.getInt(KEY_MAX_STEPS, 0);
        mEtMaxSteps.setText(String.valueOf(maxSteps));

        mBtnSave = (Button) mView.findViewById(R.id.btn_settings_save);
        mBtnSave.setOnClickListener(this);


        return mView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btn_settings_save:
                EditText etMaxSteps = (EditText) mView.findViewById(R.id.et_settings_max_steps);



                try {
                    int maxSteps = Integer.parseInt(etMaxSteps.getText().toString());

                    // save
                    SharedPreferences sharedpreferences = getContext().getSharedPreferences(MY_PREFS, getContext().MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedpreferences.edit();
                    editor.putInt(KEY_MAX_STEPS, maxSteps);
                    editor.apply();

                    Toast.makeText(getContext(), "Settings saved " ,Toast.LENGTH_LONG).show();

                } catch (Exception e) {
                    Toast.makeText(getContext(), "Error saving settings..." ,Toast.LENGTH_LONG).show();
                }

                break;

            default:
                Log.d(TAG, "unknown onclick id encountered...");
        }
    }
}
