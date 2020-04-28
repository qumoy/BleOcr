package com.ble.imgtranslator;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;


import com.ble.imgtranslator.translator.Translator;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageTranslator {
    public static String languageDir = "";
    private LanguageManager languageManager;
    private static ImageTranslator mImageTranslator = null;
    /**
     * TessBaseAPI初始化用到的第一个参数，是个目录。
     */
    private static final String DATA_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    /**
     * TessBaseAPI初始化测第二个参数，就是识别库的名字不要后缀名。
     */
    private static final String DEFAULT_LANGUAGE = "num";

    private ImageTranslator() {
    }

    /**
     * 初始化
     */
    public void init(Context context) {
        languageDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath() + "/tessdata/";
        File file = new File(languageDir);
        if (!file.exists())
            file.mkdirs();

        if (!file.isDirectory()) {
            file.delete();
            file.mkdirs();
        }

        if (!file.exists())
            throw new RuntimeException("初始化失败");
        languageManager = new LanguageManager(context);
        languageManager.clearDownload();
    }

    public static ImageTranslator getInstance() {
        if (mImageTranslator == null)
            synchronized (ImageTranslator.class) {
                if (mImageTranslator == null)
                    mImageTranslator = new ImageTranslator();
            }
        return mImageTranslator;
    }


    /**
     * 扫描结果回调用
     */
    public interface TesseractCallback {
        void onResult(String result);

        void onFail(String reason);
    }


    /**
     * 开始单行识别
     *
     * @param bmp      需要识别的图片
     * @param callBack 结果回调（携带一个String 参数即可）
     */

    public void translate(final Translator translator, final Bitmap bmp, final TesseractCallback callBack) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (checkLanguage(translator)) {
                    TessBaseAPI baseApi = new TessBaseAPI();
                    String tessdataPath = languageDir.substring(0, languageDir.length() - "tessdata/".length());
                    Log.e("LanguageManager", "tessdataPath: " + tessdataPath);
                    if (baseApi.init(tessdataPath, translator.initLanguage())) {
//                    if (baseApi.init(DATA_PATH, DEFAULT_LANGUAGE)) {
                        //设置识别模式
                        baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
                        //内容捕捉
                        Bitmap textBmp = translator.catchText(bmp);
                        if (textBmp != null) {
                            //设置要识别的图片
                            baseApi.setImage(textBmp);
                            baseApi.setVariable(TessBaseAPI.VAR_SAVE_BLOB_CHOICES, TessBaseAPI.VAR_TRUE);
                            //开始识别
                            String result = baseApi.getUTF8Text();
                            baseApi.clear();
                            baseApi.end();
                            bmp.recycle();
                            callBack.onResult(filter(translator, result));
                        } else {
                            callBack.onResult("");
                        }
                    } else {
                        callBack.onResult("");
                    }
                } else {
                    callBack.onFail("正在下载字库 " + translator.initLanguage() + " ...");
                }
            }
        }).start();
    }

    /**
     * 筛选扫描结果
     */
    public String filter(Translator translator, String str) {
        if (TextUtils.isEmpty(str) || TextUtils.isEmpty(translator.filterRule()))
            return str;
        Pattern pattern = Pattern.compile(translator.filterRule());
        Matcher matcher = pattern.matcher(str);
        StringBuffer bf = new StringBuffer();
        while (matcher.find()) {
            bf.append(matcher.group()).append(",");
        }
        int len = bf.length();
        if (len > 0) {
            bf.deleteCharAt(len - 1);
        }
        return bf.toString();
    }

    /**
     * 检查语言包
     */
    public boolean checkLanguage(Translator translator) {
        if (TextUtils.isEmpty(languageDir))
            throw new RuntimeException("ImageTranslator未初始化，you need ImageTranslator.getInstance().init(ApplicationContext)  first");
        String tessdata = languageDir + translator.initLanguage() + ".traineddata";
        File file = new File(tessdata);
        if (!file.exists()) {
            if (languageManager == null)
                throw new RuntimeException("ImageTranslator未初始化，you need ImageTranslator.getInstance().init(ApplicationContext) first");
            //下载语言包
            languageManager.downloadLanguage(translator.initLanguage());
            return false;
        } else {
            return true;
        }
    }
}
