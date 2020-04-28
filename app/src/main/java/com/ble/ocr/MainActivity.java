package com.ble.ocr;

import android.Manifest;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.ble.ocr.Crc.CrcOperateUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    CameraView mCameraView;
    RecycledImageView mImageView;
    /**
     * TessBaseAPI初始化用到的第一个参数，是个目录。
     */
    private static final String DATA_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    /**
     * 在DATA_PATH中新建这个目录，TessBaseAPI初始化要求必须有这个目录。
     */
    private static final String tessdata = DATA_PATH + File.separator + "tessdata";
    /**
     * TessBaseAPI初始化测第二个参数，就是识别库的名字不要后缀名。
     */
    private static final String DEFAULT_LANGUAGE = "num";
    /**
     * assets中的文件名
     */
    private static final String DEFAULT_LANGUAGE_NAME = DEFAULT_LANGUAGE + ".traineddata";
    /**
     * 保存到SD卡中的完整文件名
     */
    private static final String LANGUAGE_PATH = tessdata + File.separator + DEFAULT_LANGUAGE_NAME;
    private TextView mTvResult;
    private static final long MSG_REPONSE_TIME = 200 * 1000;
    private static final long MSG_TIME_DELAY = 1000;
    private static final long MSG_TIME_PERIOD = 1000;
    private static final int BLE_REQUEST_CODE = 1;
    private static final long BLE_CONNECT_TIME = 6 * 1000;
    protected static final String BLE_SP = "Ble";
    protected static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private BluetoothGatt mBluetoothGatt;
    private BletoothService mBletoothService = BletoothService.getInstance();
    private BluetoothGattCharacteristic mWriteCharacteristic, mNotifyCharacteristic;
    private BlueToothBroadcastReceiver receiver;
    private long msgSendTime;
    private boolean isMsgResponse = false;
    private Dialog mWaittingDialog;
    private Timer timer;
    private String mBleResponse = "";
    private AlertDialog mBleDisConnectDialog;
    private Timer connectTimer;
    private long connectStartTime;
    private boolean isBleConnected = true;
    private AlertDialog mBleReConnectDialog;
    private boolean isSendOcrResult = false;
    private String mConnectedAddress;
    private Button mBtnFlash;
    private boolean isFlash=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCameraView = findViewById(R.id.main_camera);
        mImageView = findViewById(R.id.main_image);
        mTvResult = findViewById(R.id.tv_result);
//        mBtnFlash = findViewById(R.id.btn_flash);
//        mBtnFlash.setOnClickListener(view -> {
//            if (!isFlash){
//                mCameraView.initCameraParams2();
//                isFlash=!isFlash;
//            }else{
//                mCameraView.initCameraParams();
//                isFlash=!isFlash;
//            }
//        });
        mCameraView.setTag(mImageView);
        mCameraView.setResultChangeListener(result -> {
            if (!TextUtils.isEmpty(result))
                runOnUiThread(() -> {
                    mTvResult.setText("扫描结果：" + result);
                    Log.e(TAG, "扫描结果：" + result + "  isSendOcrResult:" + isSendOcrResult);
                    if (isSendOcrResult) {
                        sendMsg(result);
                        isSendOcrResult = false;
                    }
                });

        });

        mBluetoothGatt = BletoothService.getBtGatt();
        //从SharedPreferences中获取已连接蓝牙mac地址
        SharedPreferences sp = getSharedPreferences(BLE_SP, MODE_PRIVATE);
        mConnectedAddress = sp.getString(EXTRAS_DEVICE_ADDRESS, "default");
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS}, 0);
            }
        }
        copyToSD(LANGUAGE_PATH, DEFAULT_LANGUAGE_NAME);
    }

    private void sendMsg(String data) {
        if (mBluetoothGatt != null && mWriteCharacteristic != null) {
            byte[] msgData = HexUtil.stringToByte(data);
            byte[] msgCmd = new byte[]{0x0d, 0x0a};
            byte[] msgSend = CrcOperateUtil.concatAll(msgData, msgCmd);
            List<byte[]> strings = BlePacketUtil.writeEntitys(msgSend);
            for (byte[] s2 : strings) {
                try {
                    Thread.sleep(20);
                    mWriteCharacteristic.setValue(s2);
                    mBluetoothGatt.writeCharacteristic(mWriteCharacteristic);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

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
                    Log.e(TAG, "msgReceiverTime: " + msgReceiverTime + "  msgSendTime:" + msgSendTime);
                    if (msgReceiverTime - msgSendTime > MSG_REPONSE_TIME) {
                        Looper.prepare();
                        showErrorDialog("错误信息", "连接超时，请检查配置！");
                        setDialogDissmiss(mWaittingDialog);
                        Looper.loop();
                    }
                }
            }
        }, MSG_TIME_DELAY, MSG_TIME_PERIOD);
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.e("AAA", "onResume: ");
        //注册蓝牙广播接受者
        initBleReceiver();
        //Gatt开启服务
        try {
            if (mBluetoothGatt == null) {
                mBluetoothGatt = BletoothService.getBtGatt();
            }
            mBluetoothGatt.discoverServices();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        Log.e(TAG, "mBluetoothGatt: " + mBluetoothGatt);
    }

    /**
     * 注册蓝牙广播接受者
     */
    protected void initBleReceiver() {
        receiver = new BlueToothBroadcastReceiver();
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BletoothService.ACTION_GATT_CONNECTED);
        filter.addAction(BletoothService.ACTION_GATT_DISCONNECTED);
        filter.addAction(BletoothService.ACTION_GATT_SERVICES_DISCOVERED);
        filter.addAction(BletoothService.ACTION_DATA_NOTIFY);
        filter.addAction(BletoothService.ACTION_DATA_WRITE);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        setDialogDissmiss(mWaittingDialog);
        Log.e("AAA", "onPause: ");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != timer) {
            timer.cancel();
        }
        Log.e("AAA", "onDestroy: ");
    }

    /**
     * 蓝牙广播接收者
     */
    private class BlueToothBroadcastReceiver extends BroadcastReceiver {

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "action: " + action);
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                Log.i(TAG, "Connect test 0 ACTION_STATE_CHANGED");
            }
            if (BletoothService.ACTION_GATT_CONNECTED.equals(action)) {
                isBleConnected = true;
                connectStartTime = 0;
                setDialogDissmiss(mWaittingDialog);
                setDialogDissmiss(mBleReConnectDialog);
                setDialogDissmiss(mBleDisConnectDialog);
                showDialog("返回信息", "蓝牙已重新连接");
                mBluetoothGatt = BletoothService.getBtGatt();
                mBluetoothGatt.discoverServices();
            } else if (BletoothService.ACTION_GATT_DISCONNECTED.equals(action)) {
                isBleConnected = false;
                setDialogDissmiss(mBleReConnectDialog);
                setDialogDissmiss(mBleDisConnectDialog);
                ToastUtil.showToast(MainActivity.this, "断开连接");
                //TODO 验证是否正确
                mBletoothService.close();
                showBleConnectDialog("蓝牙断开连接", "点击确定重新连接！");
            } else if (BletoothService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                List<BluetoothGattService> supportedGattServices = mBletoothService.getSupportedGattServices();
                Log.e(TAG, "supportedGattServices: " + supportedGattServices);
                try {
                    for (BluetoothGattService bluetoothGattService : supportedGattServices) {
                        Log.i(TAG, "bluetoothGattService.uuid:" + bluetoothGattService.getUuid());
                        List<BluetoothGattCharacteristic> characteristics = bluetoothGattService.getCharacteristics();
                        for (BluetoothGattCharacteristic characteristic : characteristics) {
                            Log.i(TAG, "BluetoothGattCharacteristic.uuid: " + characteristic.getUuid());
                            if (characteristic.getUuid().toString().equals(GattInfo.WRITE_CHARACTERISTIC)) {
                                mWriteCharacteristic = characteristic;
                                Log.e(TAG, "mWriteCharacteristic: " + mWriteCharacteristic);
                            } else if (characteristic.getUuid().toString().equals(GattInfo.NOTIFY_CHARACTERISTIC)) {
                                mNotifyCharacteristic = characteristic;
                                Log.e(TAG, "mNotifyCharacteristic: " + mNotifyCharacteristic);
                            }
                        }

                    }
                    mBletoothService.setCharacteristicNotification(mNotifyCharacteristic, true);
                    mBletoothService.readCharacteristic(mNotifyCharacteristic);
                    //休眠100ms，初始化协议栈
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else if (BletoothService.ACTION_DATA_WRITE.equals(action)) {
                //数据写入记录时间，计算超时时间
                msgSendTime = System.currentTimeMillis();
                isMsgResponse = false;
                int status = intent.getIntExtra(BletoothService.EXTRA_STATUS, 0);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(TAG, "数据写入成功");
                } else {
                    ToastUtil.showToast(MainActivity.this, "数据写入失败");
                }
            } else if (BletoothService.ACTION_DATA_NOTIFY.equals(action)) {
                //记录是否有消息返回
                isMsgResponse = true;
                setDialogDissmiss(mWaittingDialog);
                byte[] value = intent.getByteArrayExtra(BletoothService.EXTRA_DATA);
                String response = HexUtil.hexStr2Str(HexUtil.byte2hex(value, 0, value.length));
                if (TextUtils.equals(response, "ids\r\n")) {
                    isSendOcrResult = true;
                }
            }
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
                    setDialogDissmiss(mBleReConnectDialog);
                    setDialogDissmiss(mBleDisConnectDialog);
                    finish();
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    protected void showWaittingDialog() {
        mWaittingDialog = DialogUtil.showProgressDialog(MainActivity.this, "加载中");
    }

    private void setDialogDissmiss(Dialog dialog) {
        if (null != dialog && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    /**
     * 蓝牙断开提示弹窗
     *
     * @param title 设置弹窗标题
     * @param msg   设置弹窗内容
     */
    public void showBleConnectDialog(String title, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false)
                .setTitle(title)
                .setMessage(msg)
                .setNegativeButton(getResources().getString(R.string.text_negative_button), (dialog, which) -> {
                    dialog.dismiss();
                    startActivity(new Intent(MainActivity.this, BleScanActivity.class));
                })
                .setPositiveButton(getResources().getString(R.string.text_positive_button), (dialog, which) -> {
                    dialog.dismiss();
                    checkBle();
                    showWaittingDialog();
                    connectStartTime = System.currentTimeMillis();
                    checkBleConnectTimeOut();
                });
        mBleDisConnectDialog = builder.create();
        mBleDisConnectDialog.show();
    }

    /**
     * 检查蓝牙是否打开
     */
    public void checkBle() {
        //蓝牙管理，这是系统服务可以通过getSystemService(BLUETOOTH_SERVICE)的方法获取实例
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        //通过蓝牙管理实例获取适配器，然后通过扫描方法（scan）获取设备(device)
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BLE_REQUEST_CODE);
        }
        mBletoothService.connect(mConnectedAddress);
        mBluetoothGatt = BletoothService.getBtGatt();
        Log.e(TAG, "mBletoothService: " + mBletoothService);
    }

    private void checkBleConnectTimeOut() {
        //开启子线程轮训判断请求超时
        connectTimer = new Timer();
        connectTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                //判断指令是否超时
                if (!isBleConnected && connectStartTime != 0) {
                    long connectReceiverTime = System.currentTimeMillis();
                    if (connectReceiverTime - connectStartTime > BLE_CONNECT_TIME) {
                        Looper.prepare();
                        showBleTimeOutDialog("错误信息", "蓝牙连接超时，点击确定重新连接");
                        setDialogDissmiss(mWaittingDialog);
                        connectStartTime = 0;
                        Looper.loop();
                    }
                }
            }
        }, MSG_TIME_DELAY, MSG_TIME_PERIOD);
    }

    /**
     * 设置错误信息弹窗
     *
     * @param title 设置弹窗标题
     * @param msg   设置弹窗内容
     */
    public void showBleTimeOutDialog(String title, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false)
                .setTitle(title)
                .setMessage(msg)
                .setNegativeButton(getResources().getString(R.string.text_negative_button), (dialog, which) -> {
                    dialog.dismiss();
                    connectStartTime = 0;
                    if (connectTimer != null) {
                        connectTimer.cancel();
                    }
                    startActivity(new Intent(MainActivity.this, BleScanActivity.class));
                })
                .setPositiveButton(getResources().getString(R.string.text_positive_button), (dialog, which) -> {
                    dialog.dismiss();
                    checkBle();
                    showWaittingDialog();
                    connectStartTime = System.currentTimeMillis();
                    checkBleConnectTimeOut();
                });
        mBleReConnectDialog = builder.create();
        mBleReConnectDialog.show();
    }

    /**
     * 消息弹框设置
     *
     * @param title 设置弹窗标题
     * @param msg   设置弹窗内容
     */
    public void showDialog(String title, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(msg)
                .setPositiveButton(getResources().getString(R.string.text_positive_button), (dialog, which) -> {
                    dialog.dismiss();
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * 将assets中的识别库复制到SD卡中
     *
     * @param path 要存放在SD卡中的 完整的文件名。这里是"/storage/emulated/0//tessdata/num.traineddata"
     * @param name assets中的文件名 这里是 "num.traineddata"
     */
    public void copyToSD(String path, String name) {

        //如果存在就删掉
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }
        if (!f.exists()) {
            File p = new File(f.getParent());
            if (!p.exists()) {
                p.mkdirs();
            }
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        InputStream is = null;
        OutputStream os = null;
        try {
            is = this.getAssets().open(name);
            File file = new File(path);
            os = new FileOutputStream(file);
            byte[] bytes = new byte[2048];
            int len = 0;
            while ((len = is.read(bytes)) != -1) {
                os.write(bytes, 0, len);
            }
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null)
                    is.close();
                if (os != null)
                    os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
