package at.fhooe.mc.mos.ui;



import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import at.fhooe.mc.mos.R;

/**
 * Created by sstri on 09.11.2016.
 */

public class ScanActivity extends ListActivity implements View.OnClickListener {
    //constants
    private static final String TAG = ScanActivity.class.getSimpleName();
    private static final long SCAN_PERIOD = 10000;

    //variables
    private DeviceListAdapter mDeviceListAdapter = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private Button mRefresh = null;
    private Handler mHandler;
    private BluetoothManager mBluetoothManager = null;
    private BluetoothDevice mDevice = null;

    //flags
    private boolean mScanning;


    // Creates layout and initializes Bluetooth.
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDeviceListAdapter = new DeviceListAdapter();
        mHandler = new Handler();
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        setContentView(R.layout.activity_scan);
    }

    // Starts scanning and initializes list.
    @Override
    protected void onResume() {
        super.onResume();
        mRefresh = (Button) findViewById(R.id.scanActivity_button_refresh);
        mRefresh.setOnClickListener(this);
        setListAdapter(mDeviceListAdapter);
        scanForDevices(true);
    }

    //Stops scanning and cleans up.
    @Override
    protected void onPause() {
        super.onPause();
        scanForDevices(false);
        mDeviceListAdapter.clear();
    }


    //listener methods

    // Used to stop scanning and setting result for MainActivity onActivityResult
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        mDevice = mDeviceListAdapter.getDevice(position);
        if (mDevice == null) return;

        Intent intent = new Intent();
        intent.putExtra(ActivityFragment.EXTRAS_DEVICE_ADDRESS, mDevice.getAddress());
        intent.putExtra(ActivityFragment.EXTRAS_DEVICE_NAME, mDevice.getName());

        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    // Restarts scanning.
    @Override
    public void onClick(View v) {
        if (!mScanning && v.getId() == R.id.scanActivity_button_refresh) {
            mDeviceListAdapter.clear();
            scanForDevices(true);
        }
    }

    // UI methods
    // Rotates the Image in a predefined time interval using CountDownTimer
    private void rotateImage() {
        final ImageView refresh = (ImageView) findViewById(R.id.scanActivity_image_refresh);
        refresh.setVisibility(View.VISIBLE);
        CountDownTimer timer = new CountDownTimer(SCAN_PERIOD, 10) {
            int i = 1;

            @Override
            public void onTick(long millisUntilFinished) {
                refresh.setRotation(1 * i++);
                refresh.invalidate();
            }

            @Override
            public void onFinish() {
                refresh.setRotation(0);
                refresh.invalidate();
                refresh.setVisibility(View.INVISIBLE);
            }
        };
        timer.start();
    }

    //bluetooth methods

    // used for starting and stopping the scan.
    private void scanForDevices(final boolean enable) {
        if (enable) {
            rotateImage();
            mRefresh.setVisibility(View.INVISIBLE);

            // Stop scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mRefresh.setVisibility(View.VISIBLE);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    // Device scan callback
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDeviceListAdapter.addDevice(device);
                            mDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    //inner classes

    // Adapter for holding devices found through scanning.
    private class DeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mDevices;
        private LayoutInflater mInflator;

        public DeviceListAdapter() {
            super();
            mDevices = new ArrayList<BluetoothDevice>();
            mInflator = ScanActivity.this.getLayoutInflater();
        }

        // Adds a device to ArrayList if the list doesn't contain it.
        public void addDevice(BluetoothDevice device) {
            if (!mDevices.contains(device)) {
                mDevices.add(device);
            }
        }

        // Getter for the device chosen by the user.
        public BluetoothDevice getDevice(int position) {
            return mDevices.get(position);
        }

        public void clear() {
            mDevices.clear();
        }

        @Override
        public int getCount() {
            return mDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mDevices.get(i);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText("Unknown Device");
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}
