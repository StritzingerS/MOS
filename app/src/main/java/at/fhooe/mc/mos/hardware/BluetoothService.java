package at.fhooe.mc.mos.hardware;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.RunnableFuture;

import at.fhooe.mc.mos.logic.HeartRateObserver;


/**
 * Created by sstri on 09.11.2016.
 */
public class BluetoothService extends Service implements HeartRateMonitor {

    private List<HeartRateObserver> mObservers = new ArrayList<>();
    private Handler mUiHandler;

    //constants
    private static final String TAG = "BluetoothService";
    private static final UUID CLIENT_CHARACTERISTIC_CONFIGURATION = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"); //descriptor
    private static final UUID HEART_RATE_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"); //characteristics
    private static final UUID HEART_RATE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb"); //service

    //variables
    private BluetoothManager mBluetoothManager = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothGatt mBluetoothGatt = null;
    private String mBluetoothDeviceAddress = null;
    private final IBinder mBinder = new LocalBinder();

    //GATT callback
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                gatt.discoverServices();
                Log.i(TAG, "GATT connected");
            }
            if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                gatt.discoverServices();
                Log.i(TAG, "GATT disconnected");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i(TAG, services.size() + " services discovered");
            BluetoothGattCharacteristic chara = null;
            Iterator<BluetoothGattService> iterator = services.iterator();

            do {
                BluetoothGattService service = iterator.next();
                if (service.getUuid().equals(HEART_RATE)) {
                    chara = service.getCharacteristic(HEART_RATE_MEASUREMENT);
                }

            } while (iterator.hasNext() && chara == null);

            if (chara != null && chara.getDescriptor(CLIENT_CHARACTERISTIC_CONFIGURATION) != null) {
                Log.i(TAG, "BLE Heart-Rate-Profile available");
                //set notifications for server and client
                gatt.setCharacteristicNotification(chara, true);
                BluetoothGattDescriptor desc = chara.getDescriptors().get(0);
                desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(desc);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            int flag = characteristic.getProperties();
            int format;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
            }
            final int heartRate = characteristic.getIntValue(format, 1);

            Log.i(TAG,"Heartrate: " + heartRate);

            if (heartRate != 0) {
                // Valid heart rate found

                // notify
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        notifyObservers(heartRate);
                    }
                });
            }

        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind Bluetooth Service");
        return mBinder;
    }

    // Initializes the BluetoothAdapter and the BluetoothManager
    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }


        mUiHandler = new Handler(Looper.getMainLooper());

        Log.i(TAG, "BluetoothService initialized");
        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the BluetoothGattCallback
     */
    public boolean connect(final String address) {

        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            return mBluetoothGatt.connect();
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to isEnabled.");
            return false;
        }
        // We want to directly isEnabled to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        return true;
    }


    //performs cleanup.
    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "Unbind Bluetooth Service");
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        return super.onUnbind(intent);
    }

    @Override
    public void addObserver(HeartRateObserver observer) {
        mObservers.add(observer);
    }

    @Override
    public void removeObserver(HeartRateObserver observer) {
        mObservers.remove(observer);
    }

    //inner classes; is for returning an instance of the service
    public class LocalBinder extends Binder {
        public BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    public void notifyObservers(int heartRate) {
        for (HeartRateObserver o : mObservers) {
            o.heartRate(heartRate);
        }
    }
}

