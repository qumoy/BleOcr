package com.ble.ocr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.ble.ocr.MainActivity.BLE_SP;
import static com.ble.ocr.MainActivity.EXTRAS_DEVICE_ADDRESS;


public class BleScanActivity extends AppCompatActivity {

    private static final String TAG = BleScanActivity.class.getSimpleName();
    private static final long MSG_TIME_DELAY = 1000;
    private static final long MSG_TIME_PERIOD = 1000;
    private static final long MSG_REPONSE_TIME = 5 * 1000;
    private static final int BLE_REQUEST_CODE = 2;
    private static final long SCAN_PERIOD = Integer.MAX_VALUE;
    @BindView(R.id.rc_ble)
    RecyclerView mRcBleScan;
    private BluetoothAdapter mBluetoothAdapter;
    private Intent intent;
    private BletoothService mBletoothService;
    private boolean mScanning = false;
    private Handler handler = new Handler();
    private BleScanRecyclerViewAdapter mBleRecyclerViewAdapter;
    private List<BleDeviceInfo> mBleDeviceInfoList = new ArrayList<BleDeviceInfo>();
    private BlueToothBroadcastReceiver receiver;
    private BluetoothGattCharacteristic mWriteCharacteristic, mNotifyCharacteristic;
    private String mBluetoothAddress;
    private Dialog mWaittingDialog;
    private Timer timer;
    private long msgSendTime;
    private boolean isMsgResponse = false;
    //动态申请权限
    private static final int PREMISSION_REQUEST = 0;
    private String[] permissions = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
    };
    private List<String> mPermissionList = new ArrayList<>();

    private void initPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < permissions.length; i++) {
                if (ContextCompat.checkSelfPermission(BleScanActivity.this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                    mPermissionList.add(permissions[i]);
                }
            }
            if (mPermissionList.isEmpty()) {//未授权的权限为空，表示权限都已授权
//                ToastUtil.showToast(MainActivity.this,"已经授权");
            } else {//请求权限方法
                String[] permissions = mPermissionList.toArray(new String[mPermissionList.size()]);
                ActivityCompat.requestPermissions(BleScanActivity.this, permissions, PREMISSION_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        ButterKnife.bind(this);
        //权限初始化
        initPermissions();
        //初始化view
        initViews();
        //蓝牙管理，这是系统服务可以通过getSystemService(BLUETOOTH_SERVICE)的方法获取实例
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        //通过蓝牙管理实例获取适配器，然后通过扫描方法（scan）获取设备(device)
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BLE_REQUEST_CODE);
        } else {
            //开启扫描
            startBletoothService();
        }
        //检查蓝牙定位服务
        if (!isLocServiceEnable(getApplicationContext())) {
            showLocationDialog("提醒", "请开启定位服务");
        }

    }

    @SuppressLint("WrongConstant")
    private void initViews() {
        mBleRecyclerViewAdapter = new BleScanRecyclerViewAdapter(mBleDeviceInfoList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRcBleScan.setLayoutManager(linearLayoutManager);
        mRcBleScan.setAdapter(mBleRecyclerViewAdapter);
        mBleRecyclerViewAdapter.setOnItemClickListener((view, bleDeviceInfo) -> {
            Log.e(TAG, "initViews: clicked");
            if (!bleDeviceInfo.isIBeacon()) {
                mWaittingDialog = DialogUtil.showProgressDialog(BleScanActivity.this, "连接中");
                msgSendTime = System.currentTimeMillis();
                mBluetoothAddress = bleDeviceInfo.getBluetoothDevice().getAddress();
                mBletoothService.connect(mBluetoothAddress);
                SharedPreferences.Editor edit = getSharedPreferences(BLE_SP, MODE_PRIVATE).edit();
                edit.putString(EXTRAS_DEVICE_ADDRESS, mBluetoothAddress);
                edit.apply();
            } else {
                ToastUtil.showToast(BleScanActivity.this, "iBeacon不可点击！");
            }
        });
    }

    /**
     * 设置超时提醒
     */
    private void setTimeOut() {
        //开启子线程轮训判断请求超时
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                //判断指令是否超时
                if (!isMsgResponse && msgSendTime != 0) {
                    long msgReceiverTime = System.currentTimeMillis();
                    if (msgReceiverTime - msgSendTime > MSG_REPONSE_TIME) {
                        runOnUiThread(() -> {
                            showErrorDialog("错误信息", "连接超时，请检查配置！");
                            setDialogDissmiss(mWaittingDialog);
                            isMsgResponse = true;
                        });
                    }
                }
            }
        }, MSG_TIME_DELAY, MSG_TIME_PERIOD);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                startScan();
                Log.e(TAG, "onOptionsItemSelected scanLeDevice is true!!!");
                break;
            case R.id.menu_stop:
                stopScan();
                Log.e(TAG, "onOptionsItemSelected scanLeDevice is false!!!");
                break;
        }
        return true;
    }

    /**
     * 开启低功耗蓝牙服务
     */
    private void startBletoothService() {
        boolean f;
        intent = new Intent(getApplicationContext(), BletoothService.class);
        startService(intent);
        f = bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        if (!f) {
            ToastUtil.showToast(BleScanActivity.this, "Bind to BletoothService failed");
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBletoothService = ((BletoothService.LocalBinder) service).getService();

            if (!mBletoothService.initialize()) {
                ToastUtil.showToast(BleScanActivity.this, "Unable to initialize BletoothService");
                finish();
                return;
            }
            int numServices = mBletoothService.getNumServices();
            if (numServices > 0) {
                runOnUiThread(() -> Log.i(TAG, "Multiple connections!"));
            } else {
                startScan();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBletoothService = null;
        }
    };

    /**
     * 开启蓝牙扫描
     */
    private void startScan() {
        mBleDeviceInfoList.clear();
        scanLeDevice(true);
        notifyDataSetChanged();
    }

    /**
     * 停止蓝牙扫描
     */
    private void stopScan() {
        mScanning = false;
        scanLeDevice(false);
    }

    /**
     * 刷新RecyclerView中的列表数据
     */
    private void notifyDataSetChanged() {
        if (mBleRecyclerViewAdapter == null) {
            mBleRecyclerViewAdapter = new BleScanRecyclerViewAdapter(mBleDeviceInfoList);
            mRcBleScan.setAdapter(mBleRecyclerViewAdapter);
        }
        mBleRecyclerViewAdapter.notifyDataSetChanged();
    }

    private Runnable startScanRunnable = () -> {
        mScanning = false;
        stopScan();
    };

    /**
     * 开启/关闭蓝牙扫描
     *
     * @param enable true开启扫描 false关闭扫描
     */
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(startScanRunnable, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    /**
     * 蓝牙扫描回调
     */
    private final BluetoothAdapter.LeScanCallback mLeScanCallback = (BluetoothDevice device, int rssi, byte[] scanRecord) -> {
        //不能做耗时操作，特别是周围设备多的时候
        BleScanActivity.this.runOnUiThread(() -> {
            if (!BleScanActivity.this.deviceInfoExists(device.getAddress())) {
                if (device.getName() != null) {
                    mBleDeviceInfoList.add(new BleDeviceInfo(device, rssi, scanRecord));
                    notifyDataSetChanged();
                }
            } else {
                // Already in list, update RSSI info
                BleDeviceInfo deviceInfo = BleScanActivity.this.findDeviceInfo(device);
                assert deviceInfo != null;
                deviceInfo.updateParameters(rssi, scanRecord);
                notifyDataSetChanged();
            }
        });
    };

    private BleDeviceInfo findDeviceInfo(BluetoothDevice device) {
        for (int i = 0; i < mBleDeviceInfoList.size(); i++) {
            if (mBleDeviceInfoList.get(i).getBluetoothDevice().getAddress()
                    .equals(device.getAddress())) {
                return mBleDeviceInfoList.get(i);
            }
        }
        return null;
    }

    /**
     * 筛选重复的设备地址
     */
    private boolean deviceInfoExists(String address) {
        for (int i = 0; i < mBleDeviceInfoList.size(); i++) {
            if (mBleDeviceInfoList.get(i).getBluetoothDevice().getAddress()
                    .equals(address)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mBluetoothAddress = getSharedPreferences(BLE_SP, MODE_PRIVATE).getString(EXTRAS_DEVICE_ADDRESS, "default");
    }

    @Override
    protected void onResume() {
        super.onResume();
        //注册低功耗蓝牙接受者
        initBleReceiver();
        //设置Beacon连接超时
        setTimeOut();
        //回退断开蓝牙
        if (!TextUtils.equals(mBluetoothAddress, "default") && mBletoothService != null) {
            mBletoothService.disconnect(mBluetoothAddress);
            isMsgResponse = false;
            msgSendTime = 0;
        }
    }

    /**
     * 注册蓝牙接受者
     */
    private void initBleReceiver() {
        receiver = new BlueToothBroadcastReceiver();
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BletoothService.ACTION_GATT_CONNECTED);
        filter.addAction(BletoothService.ACTION_GATT_DISCONNECTED);
        filter.addAction(BletoothService.ACTION_GATT_SERVICES_DISCOVERED);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        setDialogDissmiss(mWaittingDialog);
        unregisterReceiver(receiver);
        if (null != timer) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2 && resultCode == RESULT_OK) {
            //第一次安装，蓝牙启动后开启服务
            startBletoothService();
            ToastUtil.showToast(BleScanActivity.this, "蓝牙已开启");
        } else {
            ToastUtil.showToast(BleScanActivity.this, "蓝牙未开启");
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        stopService(intent);
        mBletoothService.close();
    }

    /**
     * 蓝牙广播接收者
     */
    private class BlueToothBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "action: " + action);
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                Log.i(TAG, "Connect test 0 ACTION_STATE_CHANGED");
            }
            if (BletoothService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.e("test", "ACTION_GATT_CONNECTED: " + mBluetoothAddress);
                setDialogDissmiss(mWaittingDialog);
                ToastUtil.showToast(BleScanActivity.this, "已连接");
                Intent ocrIntent = new Intent(BleScanActivity.this, MainActivity.class);
                startActivity(ocrIntent);
            } else if (BletoothService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.e("test", "ACTION_GATT_DISCONNECTED: " + mBluetoothAddress);
                setDialogDissmiss(mWaittingDialog);
                mBletoothService.close();
                ToastUtil.showToast(BleScanActivity.this, "断开连接");
            } else if (BletoothService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                List<BluetoothGattService> supportedGattServices = mBletoothService.getSupportedGattServices();
                try {
                    for (BluetoothGattService bluetoothGattService : supportedGattServices) {
                        Log.i(TAG, "bluetoothGattService.uuid:" + bluetoothGattService.getUuid());
                        List<BluetoothGattCharacteristic> characteristics = bluetoothGattService.getCharacteristics();
                        for (BluetoothGattCharacteristic characteristic : characteristics) {
                            Log.i(TAG, "BluetoothGattCharacteristic.uuid: " + characteristic.getUuid());
                            if (characteristic.getUuid().toString().equals(GattInfo.WRITE_CHARACTERISTIC)) {
                                mWriteCharacteristic = characteristic;
                            } else if (characteristic.getUuid().toString().equals(GattInfo.NOTIFY_CHARACTERISTIC)) {
                                mNotifyCharacteristic = characteristic;
                            }
                        }
                    }
                    mBletoothService.setCharacteristicNotification(mNotifyCharacteristic, true);
                    mBletoothService.readCharacteristic(mNotifyCharacteristic);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void setDialogDissmiss(Dialog dialog) {
        if (null != dialog && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    /**
     * 设置错误信息弹窗
     *
     * @param title 设置弹窗标题
     * @param msg   设置弹窗内容
     */
    public void showErrorDialog(String title, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(getResources().getString(R.string.text_positive_button), (dialog, which) -> {
                    dialog.dismiss();
                    isMsgResponse = false;
                    msgSendTime = 0;
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * 设置错误信息弹窗
     *
     * @param title 设置弹窗标题
     * @param msg   设置弹窗内容
     */
    public void showLocationDialog(String title, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(getResources().getString(R.string.text_positive_button), (dialog, which) -> {
                    dialog.dismiss();
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    /**
     * 手机是否开启位置服务，如果没有开启那么所有app将不能使用定位功能
     */
    public static boolean isLocServiceEnable(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps || network) {
            return true;
        }
        return false;
    }
}
