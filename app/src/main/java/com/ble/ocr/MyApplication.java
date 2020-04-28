package com.ble.ocr;

import android.app.Application;

import com.ble.imgtranslator.ImageTranslator;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ImageTranslator.getInstance().init(this);
    }
}
