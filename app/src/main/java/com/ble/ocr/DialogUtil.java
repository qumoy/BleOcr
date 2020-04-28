package com.ble.ocr;

import android.app.Dialog;
import android.content.Context;
import android.widget.TextView;

import java.util.Objects;

/**
 * Author Qumoy
 * Create Date 2019/7/17
 * Description：
 * Modifier:
 * Modify Date:
 * Bugzilla Id:
 * Modify Content:
 */
public class DialogUtil {

    /**
     * 加载等待弹窗
     */
    public static Dialog showProgressDialog(Context context, String title) {
        Dialog progressDialog = new Dialog(context, R.style.progress_dialog);
        progressDialog.setContentView(R.layout.layout_progress_dialog);
        progressDialog.setCancelable(false);
        Objects.requireNonNull(progressDialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        TextView msg = progressDialog.findViewById(R.id.id_tv_loadingmsg);
        msg.setText(title);
        progressDialog.show();
        return progressDialog;
    }

    /**
     * 加载等待弹窗,可取消弹窗
     */
    public static Dialog showProgressCancelableDialog(Context context, String title) {
        Dialog progressDialog = new Dialog(context, R.style.progress_dialog);
        progressDialog.setContentView(R.layout.layout_progress_dialog);
        progressDialog.setCancelable(true);
        Objects.requireNonNull(progressDialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        TextView msg = progressDialog.findViewById(R.id.id_tv_loadingmsg);
        msg.setText(title);
        progressDialog.show();
        return progressDialog;
    }

}
