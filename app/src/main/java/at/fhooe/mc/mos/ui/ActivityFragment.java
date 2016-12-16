package at.fhooe.mc.mos.ui;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.api.model.StringList;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import at.fhooe.mc.mos.R;
import at.fhooe.mc.mos.hardware.AndroidBarometer;
import at.fhooe.mc.mos.hardware.AndroidPedometer;
import at.fhooe.mc.mos.hardware.BluetoothService;
import at.fhooe.mc.mos.logic.AltitudeManager;
import at.fhooe.mc.mos.logic.HeartRateManager;
import at.fhooe.mc.mos.logic.ExerciseManager;
import at.grabner.circleprogress.CircleProgressView;
import at.grabner.circleprogress.TextMode;

/**
 * Fragment for starting and stopping a activity session.
 */
public class ActivityFragment extends Fragment implements PedometerView, HeartRateView, AltitudeView, View.OnClickListener {

    private static final String TAG = ActivityFragment.class.getSimpleName();
    private static final int PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;

    private View mView;
    private Button mBtnStart;
    private Button mBtnStop;
    private Button mBtnBluetooth;

    private CircleProgressView mCircleView;
    private ExerciseManager mExerciseManager;
    private HeartRateManager mHeartRateManager;
    private AltitudeManager mAltitudeManager;
    private TextView mTVCalories;
    private TextView mTVDistance;
    private TextView mTVHeartRate;
    private TextView mTVAvgHeartRate;
    private TextView mTVHrMaxPercentage;
    private TextView mTVAltitude;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_GET_DEVICE = 2;
    public static final String EXTRAS_DEVICE_NAME = "bluetoothleheartrate.ble.device.name";
    public static final String EXTRAS_DEVICE_ADDRESS = "bluetoothleheartrate.ble.device.address";

    private boolean mBleServiceIsBound = false;

    //service connections
    private String mDeviceAddress = null;
    private String mDeviceName = null; //reserved for future uses
    private BluetoothService mBluetoothService = null;
    private final ServiceConnection mBluetoothServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "BluetoothService connected");
            mBluetoothService = ((BluetoothService.LocalBinder) service).getService();
            mBluetoothService.initialize();
            mBluetoothService.connect(mDeviceAddress);

            // String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            // DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference().child("users").child(uid).child("steps");
            mHeartRateManager = new HeartRateManager(ActivityFragment.this, getContext(), mBluetoothService, null);
            mHeartRateManager.start();

            mExerciseManager.setHeartRateManager(mHeartRateManager);
            setHeartRateButtonVisibility(false);
            setHeartRateTextViewsVisibility(true);
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothService = null;
        }
    };

    public void setHeartRateButtonVisibility(boolean enable){
        if(enable) {
            mBtnBluetooth.setVisibility(View.VISIBLE);
        }else{
            mBtnBluetooth.setVisibility(View.GONE);
        }
    }

    public void setHeartRateTextViewsVisibility(boolean enable){
        int visibility;
        if(enable) {
            visibility=View.VISIBLE;
        }else{
            visibility=View.GONE;
        }
        mView.findViewById(R.id.tv_activity_heartrate).setVisibility(visibility);
        mView.findViewById(R.id.tv_activity_heartrate_text).setVisibility(visibility);
        mView.findViewById(R.id.tv_activity_average_heartrate).setVisibility(visibility);
        mView.findViewById(R.id.tv_activity_average_heartrate_text).setVisibility(visibility);
        mView.findViewById(R.id.tv_activity_max_heartrate).setVisibility(visibility);
        mView.findViewById(R.id.tv_activity_max_heartrate_text).setVisibility(visibility);
    }

    public ActivityFragment() {
        // Required empty public constructor
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "On Destroy");
        bindBleService(false);
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
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference().child("users").child(uid).child("exercise");
        // needs application context to prevent memory leaks

        mExerciseManager = new ExerciseManager(this, getContext(), AndroidPedometer.getInstance(getActivity().getApplicationContext()), databaseRef);
        mAltitudeManager = new AltitudeManager(this, getContext(), AndroidBarometer.getInstance(getActivity().getApplicationContext()));
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

        //TextViews
        mTVCalories = (TextView) mView.findViewById(R.id.tv_activity_calories);
        mTVDistance = (TextView) mView.findViewById(R.id.tv_activity_distance);
        mTVHeartRate = (TextView) mView.findViewById(R.id.tv_activity_heartrate);
        mTVAvgHeartRate = (TextView) mView.findViewById(R.id.tv_activity_average_heartrate);
        mTVHrMaxPercentage = (TextView) mView.findViewById(R.id.tv_activity_max_heartrate);
        mTVAltitude = (TextView) mView.findViewById(R.id.tv_activity_altitude);

        // Buttons
        mBtnStart = (Button) mView.findViewById(R.id.btn_activity_start);
        mBtnStart.setOnClickListener(this);

        mBtnStop = (Button) mView.findViewById(R.id.btn_activity_stop);
        mBtnStop.setOnClickListener(this);

        mBtnBluetooth = (Button) mView.findViewById(R.id.btn_activity_enable_bluetooth);
        mBtnBluetooth.setOnClickListener(this);

        setHeartRateTextViewsVisibility(false);
        setHeartRateButtonVisibility(true);
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
                mExerciseManager.startCounting();
                mAltitudeManager.start();

                mBtnStart.setEnabled(false);
                mBtnStop.setEnabled(true);

                setHeartRateButtonVisibility(false);

                if (mHeartRateManager != null) {
                    mHeartRateManager.start();
                }

                break;
            case R.id.btn_activity_stop:
                mBtnStart.setEnabled(false);
                mBtnStop.setEnabled(false);

                if (mHeartRateManager != null) {
                    mHeartRateManager.stop();
                }

                mExerciseManager.stopCounter();
                mAltitudeManager.stop();

                // saving online
                mExerciseManager.saveData();

                setHeartRateButtonVisibility(true);

                break;
            case R.id.btn_activity_enable_bluetooth:
                // connect to BLE
                if(mHeartRateManager == null) {
                    if (Build.VERSION.SDK_INT >= 23) {
                        requestPermission();
                    } else {
                        enableBluetoothAndStartScan();
                    }
                }
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
            Toast.makeText(getContext(), "Exercise data saved online", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), "Error saving data", Toast.LENGTH_LONG).show();
        }

        // TODO: handle errors
        // treat as success for now...
        mExerciseManager.reset();
        if(mHeartRateManager != null) {
            mHeartRateManager.reset();
        }

        // enable button
        mBtnStart.setEnabled(true);
        mBtnStop.setEnabled(false);

        // reset view
        mCircleView.setValueAnimated(0);
        mTVCalories.setText(String.valueOf(0));
        mTVDistance.setText(String.valueOf(0)+"km");
        mTVAvgHeartRate.setText("-");
        mTVHrMaxPercentage.setText("-");
        mTVHeartRate.setText("-");
        mTVAltitude.setText("-");
    }

    // Callback for Activities started with a specific request Code.
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            // finish();
            return;
        } else if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK) {
            startActivityForResult(new Intent(getActivity(), ScanActivity.class), REQUEST_GET_DEVICE);
        }

        if (requestCode == REQUEST_GET_DEVICE && resultCode == Activity.RESULT_OK) {
            mDeviceAddress = data.getStringExtra(EXTRAS_DEVICE_ADDRESS);
            mDeviceName = data.getStringExtra(EXTRAS_DEVICE_NAME);
            bindBleService(true);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    /**
     * Once this method is called, Bluetooth is going to be activated by the user.
     * If BT is already enabled, the ScanActivity gets started.
     * If BT is not enabled the ScanActivity gets started through the {@link #onActivityResult(int, int, Intent)} Callback.
     *
     * @return False is returned if errors occurred.
     */
    private boolean enableBluetoothAndStartScan() {
        BluetoothManager bluetoothManager = (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (!getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(getContext(), "Ble not supported", Toast.LENGTH_SHORT).show();
        }

        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            startActivityForResult(new Intent(getActivity(), ScanActivity.class), REQUEST_GET_DEVICE);
        }
        return true;
    }

    /**
     * This method is used universally to bind or unbind from the BluetoothService.
     * Further workflow is specified trough the {@link #mBluetoothServiceConnection}.
     */
    private void bindBleService(boolean bind) {
        if (bind) {
            if (!mBleServiceIsBound)
                getContext().bindService(new Intent(getContext(), BluetoothService.class), mBluetoothServiceConnection, Activity.BIND_AUTO_CREATE);
            mBleServiceIsBound = true;
        } else if (mBleServiceIsBound) {
            getContext().unbindService(mBluetoothServiceConnection);
            mBleServiceIsBound = false;
        }
    }

    @Override
    public void currentHeartRate(int heartRate) {
        //Toast.makeText(getContext(), "HeartRate: " + heartRate, Toast.LENGTH_SHORT).show();
        if(heartRate==0){
            mTVHeartRate.setText("-");
        }else {
            mTVHeartRate.setText(String.valueOf(heartRate));
        }
    }

    @Override
    public void currentCalories(int currentCalories) {
        Log.i(TAG, "Calories: " + currentCalories);
        mTVCalories.setText(String.valueOf(currentCalories));
    }

    @Override
    public void currentDistance(double currentDistance) {
        Log.i(TAG, "Distance: " + currentDistance);
        mTVDistance.setText(String.valueOf(currentDistance)+"km");
    }

    @Override
    public void currentAvgHeartRate(int avgHeartRate){
        mTVAvgHeartRate.setText(String.valueOf(avgHeartRate));
    }

    @Override
    public void currentHrMaxPercentage(int hrMaxPercentage){
        mTVHrMaxPercentage.setText(String.valueOf(hrMaxPercentage)+"%");
    }

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

        } else {
            // permission is granted
            enableBluetoothAndStartScan();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted
                    enableBluetoothAndStartScan();

                } else {

                    // permission denied
                    Toast.makeText(getContext(), "You need this permission to get access to the heart rate sensor", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void currentAltitude(float altitude) {
        // no commas
        mTVAltitude.setText(String.valueOf((int) altitude));
    }
}
