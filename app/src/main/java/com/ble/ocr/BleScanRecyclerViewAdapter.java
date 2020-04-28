package com.ble.ocr;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Author Qumoy
 * Create Date 2019/7/24
 * Description：
 * Modifier:
 * Modify Date:
 * Bugzilla Id:
 * Modify Content:
 */
public class BleScanRecyclerViewAdapter extends RecyclerView.Adapter<BleScanRecyclerViewAdapter.BleViewHolder> {

    private List<BleDeviceInfo> mBleDeviceList;

    public BleScanRecyclerViewAdapter(List<BleDeviceInfo> bleDeviceList) {
        mBleDeviceList = bleDeviceList;
    }

    @NonNull
    @Override
    public BleViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_device, viewGroup, false);
        BleViewHolder bleViewHolder = new BleViewHolder(view);
        return bleViewHolder;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull BleViewHolder viewHolder, int i) {
        if (!mBleDeviceList.get(i).isIBeacon()) {
            viewHolder.mTvName.setText(mBleDeviceList.get(i).getBluetoothDevice().getName());
            viewHolder.mTvName.setTextColor(Color.BLACK);
            viewHolder.mTvMajor.setTextColor(Color.BLACK);
            viewHolder.mTvMinor.setTextColor(Color.BLACK);
            viewHolder.mTvUuid.setTextColor(Color.BLACK);
        }
        viewHolder.mTvRssi.setText(String.valueOf(mBleDeviceList.get(i).getRssi()));
        viewHolder.mTvMajor.setText("设备Major：" + mBleDeviceList.get(i).getMajor());
        viewHolder.mTvMinor.setText("设备Minor：" + mBleDeviceList.get(i).getMinor());
        viewHolder.mTvUuid.setText("设备UUID：" + mBleDeviceList.get(i).getUuid());
        Log.e("BleScanActivity", "onBindViewHolder: " + mBleDeviceList.get(i).getUuid());
//        viewHolder.mTvUuid.setText("设备UUID：" + mBleDeviceList.get(i).getScanData());
        viewHolder.mLayout.setOnClickListener(new MyOnClickListener(mBleDeviceList.get(i)));
    }

    @Override
    public int getItemCount() {
        return mBleDeviceList.size();
    }

    class BleViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.iv_image)
        ImageView mIvRssi;
        @BindView(R.id.tv_rssi)
        TextView mTvRssi;
        @BindView(R.id.tv_name)
        TextView mTvName;
        @BindView(R.id.tv_major)
        TextView mTvMajor;
        @BindView(R.id.tv_minor)
        TextView mTvMinor;
        @BindView(R.id.tv_uuid)
        TextView mTvUuid;
        @BindView(R.id.layout)
        LinearLayout mLayout;

        BleViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }

    private class MyOnClickListener implements View.OnClickListener {

        private BleDeviceInfo bleDeviceInfo;

        public MyOnClickListener(BleDeviceInfo bleDeviceInfo) {
            this.bleDeviceInfo = bleDeviceInfo;
        }

        @Override
        public void onClick(View v) {
            onItemClickListener.OnItemClick(v, bleDeviceInfo);
        }
    }

    public void setOnItemClickListener(BleScanRecyclerViewAdapter.onItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    private onItemClickListener onItemClickListener;

    public interface onItemClickListener {
        void OnItemClick(View view, BleDeviceInfo bleDeviceInfo);
    }
}
