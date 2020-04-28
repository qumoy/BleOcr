package com.ble.ocr;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * Created by Qumoy on 2020/1/12.
 * <p>
 * 扫描时，如果需要将正在处理的bitmap实时显示到一个ImageView,因为线程冲突经常会在一个Bitmap已经被释放，ImageView还在引用，会报错崩溃
 * 为了保证bimap及时被释放，又不让ImageView报错崩溃，所以这里捕获一下异常
 */

public class RecycledImageView extends AppCompatImageView {

    public RecycledImageView(Context context) {
        super(context);
    }

    public RecycledImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RecycledImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        try {
            super.onDraw(canvas);
        } catch (Exception e) {
        }
    }
}
