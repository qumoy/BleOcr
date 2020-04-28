package com.ble.ocr;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;


import java.util.List;

public class BletoothService extends Service {

    private static final String TAG = "BLETOOTHSERVICE";
    //BLE
    private BluetoothManager mBlueToothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private String mBluetoothDeviceAddress;

    public final static String ACTION_GATT_CONNECTED = "com.cks.pda.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.cks.pda.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.cks.pda.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_READ = "com.cks.pda.ACTION_DATA_READ";
    public final static String ACTION_DATA_NOTIFY = "com.cks.pda.ACTION_DATA_NOTIFY";
    public final static String ACTION_DATA_WRITE = "com.cks.pda.ACTION_DATA_WRITE";
    public final static String EXTRA_DATA = "com.cks.pda.EXTRA_DATA";
    public final static String EXTRA_UUID = "com.cks.pda.EXTRA_UUID";
    public final static String EXTRA_STATUS = "com.cks.pda.EXTRA_STATUS";
    public final static String EXTRA_ADDRESS = "com.cks.pda.EXTRA_ADDRESS";
    private volatile boolean mBusy = false;

    /**
     * GATT client callbacks
     */
    public BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (mBluetoothGatt == null) {
                Log.e(TAG, "mBluetoothGatt not created");
                return;
            }
            BluetoothDevice device = gatt.getDevice();
            String address = device.getAddress();
            Log.d(TAG, "onConnectionStateChange (" + address + ") " + newState +
                    " status: " + status);

            try {
                switch (newState) {
                    case BluetoothGatt.STATE_CONNECTED:
                        Log.d(TAG, "onConnectionStateChange: BluetoothGatt.STATE_CONNECTED");
                        broadcastUpdate(ACTION_GATT_CONNECTED, address, status);
                        break;
                    case BluetoothGatt.STATE_DISCONNECTED:
                        Log.d(TAG, "onConnectionStateChange: BluetoothGatt.STATE_DISCONNECTED");
                        broadcastUpdate(ACTION_GATT_DISCONNECTED, address, status);
                        break;
                    default:
                        break;
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            BluetoothDevice device = gatt.getDevice();
            broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED, device.getAddress(), status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            broadcastUpdate(ACTION_DATA_READ, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            broadcastUpdate(ACTION_DATA_WRITE, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            broadcastUpdate(ACTION_DATA_NOTIFY, characteristic, BluetoothGatt.GATT_SUCCESS);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            mBusy = false;
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            mBusy = false;
        }
    };
    private static BletoothService mThis;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Manage the BLE service
     */
    public class LocalBinder extends Binder {
        public BletoothService getService() {
            return BletoothService.this;
        }
    }

    private final IBinder binder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    /**
     * After using a given BLE device, the app must call this method to ensure
     * resources are released properly.
     */
    public synchronized void close() {
        Log.e(TAG, "mBluetoothGatt: " + mBluetoothGatt);
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        Log.e(TAG, "mBluetoothGatt: " + mBluetoothGatt);
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     */
    public boolean initialize() {
        //For API level 18 and above,get a reference to BluetoothAdapter through
        //BluetoothManager
        mThis = this;
        if (mBlueToothManager == null) {
            mBlueToothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBlueToothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager");
                return false;
            }
        }
        mBluetoothAdapter = mBlueToothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a  BluetoothManager");
            return false;
        }
        return true;
    }

    public void broadcastUpdate(final String action, final String address, final int status) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_ADDRESS, address);
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
        mBusy = false;
    }

    public void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic, final int status) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());
        intent.putExtra(EXTRA_DATA, characteristic.getValue());
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
        mBusy = false;
    }

    private boolean checkGatt() {
        if (mBluetoothAdapter == null) {
            return false;
        }
        if (mBluetoothGatt == null) {
            return false;
        }
        if (mBusy) {
            return false;
        }
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        close();
    }
    //
    // GATT API
    //

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read
     * result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (!checkGatt()) {
            return;
        }
        mBusy = true;
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, byte b) {
        if (!checkGatt()) {
            return false;
        }
        byte[] val = new byte[1];
        val[0] = b;
        characteristic.setValue(val);
        mBusy = true;
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] val) {
        if (!checkGatt()) {
            return false;
        }
        characteristic.setValue(val);
        mBusy = true;
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, boolean b) {
        if (!checkGatt()) {
            return false;
        }
        byte[] val = new byte[1];
        val[0] = (byte) (b ? 1 : 0);
        characteristic.setValue(val);
        mBusy = true;
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (!checkGatt()) {
            return false;
        }
        mBusy = true;
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    /**
     * Retrieves the number of GATT services on the connected device. This should
     * be invoked only after {@code BluetoothGatt#discoverServices()} completes
     * successfully.
     *
     * @return A {@code integer} number of supported services.
     */
    public int getNumServices() {
        if (mBluetoothGatt == null)
            return 0;

        return mBluetoothGatt.getServices().size();
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This
     * should be invoked only after {@code BluetoothGatt#discoverServices()}
     * completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) {
            return null;
        }
        return mBluetoothGatt.getServices();
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enable         If true, enable notification. False otherwise.
     */
    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable) {
        if (!checkGatt()) {
            return false;
        }
        boolean ok = false;
        if (mBluetoothGatt.setCharacteristicNotification(characteristic, enable)) {
            BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(GattInfo.CLIENT_CHARACTERISTIC_CONFIG);
            if (clientConfig != null) {
                if (enable) {
                    ok = clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                } else {
                    ok = clientConfig.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                }
                if (ok) {
                    mBusy = true;
                    ok = mBluetoothGatt.writeDescriptor(clientConfig);
                }
            }
        }
        return ok;
    }

    public boolean isNotificationEnable(BluetoothGattCharacteristic characteristic) {
        if (!checkGatt()) {
            return false;
        }
        BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(GattInfo.CLIENT_CHARACTERISTIC_CONFIG);
        if (clientConfig == null) {
            return false;
        }
        return clientConfig.getValue() == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The
     * connection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        int connectionState = mBlueToothManager.getConnectionState(device, BluetoothProfile.GATT);
        //Previously connected device.try to reconnect.
        if (connectionState == BluetoothProfile.STATE_DISCONNECTED) {
            if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
                Log.d(TAG, "Re-use GATT connection");
                if (mBluetoothGatt.connect()) {
                    return true;
                } else {
                    Log.w(TAG, "GATT re-connect failed.");
                    return false;
                }
            }

            if (device == null) {
                Log.w(TAG, "Device not found.  Unable to connect.");
                return false;
            }
            // We want to directly connect to the device, so we are setting the
            // autoConnect parameter to false.
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
            //重新连接注释掉此段代码，原因待考究
//            mBluetoothDeviceAddress = address;
        } else {
            Log.w(TAG, "Attempt to connect in state: " + connectionState);
            return false;
        }
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The
     * disconnection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */

    public void disconnect(String address) {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "disconnect: BluetoothAdapter not initialized");
            return;
        }
        BluetoothDevice remoteDevice = mBluetoothAdapter.getRemoteDevice(address);
        int connectionState = mBlueToothManager.getConnectionState(remoteDevice, BluetoothProfile.GATT);
        if (mBluetoothGatt != null) {
            if (connectionState != BluetoothProfile.STATE_DISCONNECTED) {
                mBluetoothGatt.disconnect();
            } else {
                // Log.w(TAG, "Attempt to disconnect in state: " + connectionState);
            }
        }
    }

    public int numConnectedDevices() {
        int n = 0;
        if (mBluetoothGatt != null) {
            List<BluetoothDevice> deviceList = mBlueToothManager.getConnectedDevices(BluetoothProfile.GATT);
            n = deviceList.size();
        }
        return n;
    }

    public static BluetoothGatt getBtGatt() {
        return mThis.mBluetoothGatt;
    }

    public static BluetoothManager getBtManager() {
        return mThis.mBlueToothManager;
    }

    public static BletoothService getInstance() {
        return mThis;
    }

    public boolean waitIdle(int timeout) {
        timeout /= 10;
        while (--timeout > 0) {
            if (mBusy)
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            else
                break;
        }

        return timeout > 0;
    }
}
