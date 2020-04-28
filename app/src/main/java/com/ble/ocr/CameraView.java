package com.ble.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import com.ble.imgtranslator.ImageTranslator;
import com.ble.imgtranslator.translator.PhoneNumberTranslator;
import com.ble.imgtranslator.translator.Translator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class CameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private final String TAG = "CameraView";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private boolean isPreviewOn;
    private Translator translator;
    //默认预览尺寸
    private int imageWidth = 1920;
    private int imageHeight = 1080;
    //帧率
    private int frameRate = 30;
    private ImageView hintImage;
    private String oldResult = "";

    public CameraView(Context context) {
        super(context);
        init();
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mHolder = getHolder();
        //设置SurfaceView 的SurfaceHolder的回调函数
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //Surface创建时开启Camera
        openCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //设置Camera基本参数
        if (mCamera != null) {
            initCameraParams();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            release();
        } catch (Exception e) {
        }
    }


    public boolean isScanning = false;
    private long starTime, endTime;
    private List<String> resultList = new ArrayList<>();
    private int reslultSize = 0;
    private String resultKey = "";
    private int resultValue = -1;

    /**
     * Camera帧数据回调用
     */
    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        //识别中不处理其他帧数据
        if (!isScanning) {
            isScanning = true;
            new Thread(() -> {
                try {
                    Log.d("scantest", "-------------Start------------------");
                    starTime = System.currentTimeMillis();
                    //获取Camera预览尺寸
                    Camera.Size size = camera.getParameters().getPreviewSize();
                    int left = (int) (size.width / 2 - DpUtil.dip2px(getContext(), 20));
                    int top = (int) (size.height / 2 - DpUtil.dip2px(getContext(), 80));
                    int right = (int) (left + DpUtil.dip2px(getContext(), 40));
                    int bottom = (int) (top + DpUtil.dip2px(getContext(), 160));
                    //将帧数据转为bitmap
                    final YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                    if (image != null) {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        image.compressToJpeg(new Rect(left, top, right, bottom), getQuality(size.height), stream);
                        Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                        if (bmp == null) {
                            isScanning = false;
                            return;
                        }
                        if (translator == null) {
                            if (getTag() != null) {
                                if (getTag() instanceof ImageView)
                                    hintImage = (ImageView) getTag();
                            }
                            translator = new PhoneNumberTranslator(hintImage);
                        }

                        //开始识别
                        ImageTranslator.getInstance().translate(translator, rotateToDegrees(bmp, 90), new ImageTranslator.TesseractCallback() {
                            @Override
                            public void onResult(String result) {
                                Log.d("scantest", "扫描结果：  " + result.replaceAll(" ", ""));
                                Log.d("scantest", "-------------End------------------");
                                if (result.replaceAll(" ", "").length() == 10) {
                                    Log.e("MainActivity", "oldResult: " + oldResult + "  " + result);
                                    if (resultList.size() == 10) {
                                        Map<String, Integer> map = new HashMap<>();
                                        for (String str : resultList) {
                                            int i = 1; //定义一个计数器，用来记录重复数据的个数
                                            if (map.get(str) != null) {
                                                i = map.get(str) + 1;
                                            }
                                            map.put(str, i);
                                        }
                                        Log.e("MainActivity", "" + map.toString());
                                        Iterator iter = map.entrySet().iterator();
                                        while (iter.hasNext()) {
                                            Map.Entry entry = (Map.Entry) iter.next();
                                            String key = (String) entry.getKey();
                                            int value = (int) entry.getValue();
                                            if (value > resultValue) {
                                                resultValue = value;
                                                resultKey = key;
                                            }
                                            Log.e("MainActivity", "key:" + key + "   value:" + value);
                                            Log.e("MainActivity", "resultKey:" + resultKey + "   resultValue:" + resultValue);
                                        }
//                                        Log.e("MainActivity", "resultKey:" + resultKey + "   resultValue:" + resultValue);
                                        resultChangeListener.obtionResult(resultKey);
                                        resultValue=-1;
                                        resultList.clear();
                                    } else {
                                        resultList.add(result);
                                    }

//                                    String result2 = result.replaceAll(" ", "");
//                                    if (TextUtils.isEmpty(oldResult)) {
//                                        oldResult = result2;
//                                    } else {
//                                        Log.e("MainActivity", "result: " + oldResult + "  " + result2);
//                                        if (TextUtils.equals(oldResult, result2)) {
//                                            resultChangeListener.obtionResult(result2);
//                                        }
//                                        oldResult = result2;
//                                    }
                                }
                                isScanning = false;
                            }

                            @Override
                            public void onFail(String reason) {
                                Log.d("scantest", "解析失败：  " + reason);
                                Log.d("scantest", "-------------End------------------");
                                isScanning = false;
                            }
                        });
                    } else {
                        isScanning = false;
                    }
                } catch (Exception ex) {
                    Log.d("scantest", ex.getMessage());
                    isScanning = false;
                }

            }).start();
        }

    }

    //压缩比例
    private int getQuality(int width) {
        int quality = 100;
        if (width > 480) {
            float w = 480 / (float) width;
            quality = (int) (w * 100);
        }
        return quality;
    }


    /**
     * 图片旋转
     *
     * @param tmpBitmap
     * @param degrees
     * @return
     */
    public static Bitmap rotateToDegrees(Bitmap tmpBitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setRotate(degrees);
        return Bitmap.createBitmap(tmpBitmap, 0, 0, tmpBitmap.getWidth(), tmpBitmap.getHeight(), matrix,
                true);
    }


    /**
     * 摄像头配置
     */
    public void initCameraParams() {
        stopPreview();
        Camera.Parameters camParams = mCamera.getParameters();
        List<Camera.Size> sizes = camParams.getSupportedPreviewSizes();
        for (int i = 0; i < sizes.size(); i++) {
            if ((sizes.get(i).width >= imageWidth && sizes.get(i).height >= imageHeight) || i == sizes.size() - 1) {
                imageWidth = sizes.get(i).width;
                imageHeight = sizes.get(i).height;
//                Log.v(TAG, "Changed to supported resolution: " + imageWidth + "x" + imageHeight);
                break;
            }
        }
//        camParams.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        camParams.setPreviewSize(imageWidth, imageHeight);
        camParams.setPictureSize(imageWidth, imageHeight);
//        Log.v(TAG, "Setting imageWidth: " + imageWidth + " imageHeight: " + imageHeight + " frameRate: " + frameRate);

        camParams.setPreviewFrameRate(frameRate);
//        Log.v(TAG, "Preview Framerate: " + camParams.getPreviewFrameRate());

        mCamera.setParameters(camParams);
        //取到的图像默认是横向的，这里旋转90度，保持和预览画面相同
        mCamera.setDisplayOrientation(90);
        // Set the holder (which might have changed) again
        startPreview();
    }

    /**
     * 开始预览
     */
    public void startPreview() {
        try {
            mCamera.setPreviewCallback(this);
            mCamera.setPreviewDisplay(mHolder);//set the surface to be used for live preview
            mCamera.startPreview();
            mCamera.autoFocus(autoFocusCB);
        } catch (IOException e) {
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 停止预览
     */
    public void stopPreview() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
        }
    }

    /**
     * 打开指定摄像头
     */
    public void openCamera() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int cameraId = 0; cameraId < Camera.getNumberOfCameras(); cameraId++) {
            Camera.getCameraInfo(cameraId, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                try {
                    mCamera = Camera.open(cameraId);
                } catch (Exception e) {
                    if (mCamera != null) {
                        mCamera.release();
                        mCamera = null;
                    }
                }
                break;
            }
        }
    }

    /**
     * 摄像头自动聚焦
     */
    Camera.AutoFocusCallback autoFocusCB = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            postDelayed(doAutoFocus, 1000);
        }
    };
    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (mCamera != null) {
                try {
                    mCamera.autoFocus(autoFocusCB);
                } catch (Exception e) {
                }
            }
        }
    };

    /**
     * 释放
     */
    public void release() {
        if (isPreviewOn && mCamera != null) {
            isPreviewOn = false;
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void setResultChangeListener(ResultChangeListener resultChangeListener) {
        this.resultChangeListener = resultChangeListener;
    }

    private ResultChangeListener resultChangeListener;

    public interface ResultChangeListener {
        void obtionResult(String result);
    }

    public void setFalsh(boolean falsh) {
        isFalsh = falsh;
    }

    public boolean isFalsh;

}
