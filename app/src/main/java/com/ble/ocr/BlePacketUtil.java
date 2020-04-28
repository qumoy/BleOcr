package com.ble.ocr;

import android.util.Log;

import java.util.LinkedList;
import java.util.List;

/**
 * @Author Qumoy
 * @Date 2019/6/23
 * @Modifier: Modify Date:
 * Bugzilla Id:
 * Modify Content:
 */
public class BlePacketUtil {
    /**
     * 默认一包发送20字节数据
     */
    private static final int BUFFER_SIZE = 20;

    /**
     * 分包发送数据
     *
     * @param data 发送数据
     */
    public static List<byte[]> writeEntitys(byte[] data) {
        if (data == null) {
            return null;
        }
        List<byte[]> packDataList = new LinkedList<byte[]>();
        int index = 0;
        int runSize = 0;
        int lastDataSize = 0;
        int length = data.length;
        while (index < length) {
            byte[] txBuffer = new byte[BUFFER_SIZE];
            for (int i = 0; i < BUFFER_SIZE; i++) {
                if (index < length) {
                    txBuffer[i] = data[index++];
                }
                runSize++;
            }
            if (length == index) {
                lastDataSize = BUFFER_SIZE - (runSize - index);
                byte[] lastBuffer = new byte[lastDataSize];
                System.arraycopy(txBuffer, 0, lastBuffer, 0, lastDataSize);
//                packDataList.add(new String(lastBuffer));
                packDataList.add(lastBuffer);
                Log.i("QrActivity", HexUtil.byte2hex(lastBuffer, 0, lastBuffer.length));
            } else {
//                packDataList.add(new String(txBuffer));
                packDataList.add(txBuffer);
                Log.i("QrActivity", HexUtil.byte2hex(txBuffer, 0, txBuffer.length));
            }

        }
        return packDataList;
    }

    /**
     * 分包发送数据
     *
     * @param data 发送数据
     */
    public static List<String> writeEntity(byte[] data) {
        if (data == null) {
            return null;
        }
        List<String> packDataList = new LinkedList<String>();
        int index = 0;
        int runSize = 0;
        int lastDataSize = 0;
        int length = data.length;
        while (index < length) {
            byte[] txBuffer = new byte[BUFFER_SIZE];
            for (int i = 0; i < BUFFER_SIZE; i++) {
                if (index < length) {
                    txBuffer[i] = data[index++];
                }
                runSize++;
            }
            if (length == index) {
                lastDataSize = BUFFER_SIZE - (runSize - index);
                byte[] lastBuffer = new byte[lastDataSize];
                System.arraycopy(txBuffer, 0, lastBuffer, 0, lastDataSize);
                packDataList.add(new String(lastBuffer));
//                packDataList.add(lastBuffer);
                Log.i("QrActivity", HexUtil.byte2hex(lastBuffer, 0, lastBuffer.length));
            } else {
                packDataList.add(new String(txBuffer));
//                packDataList.add(txBuffer);
                Log.i("QrActivity", HexUtil.byte2hex(txBuffer, 0, txBuffer.length));
            }

        }
        return packDataList;
    }

    /**
     * 将byte数组以16进制字符显示
     *
     * @param b
     */
    public static String showResult16Str(byte[] b) {
        if (b == null) {
            return "";
        }
        String rs = "";
        int bl = b.length;
        byte bt;
        String bts = "";
        int btsl;
        for (int i = 0; i < bl; i++) {
            bt = b[i];
            bts = Integer.toHexString(bt);
            btsl = bts.length();
            if (btsl > 2) {
                bts = bts.substring(btsl - 2).toUpperCase();
            } else if (btsl == 1) {
                bts = "0" + bts.toUpperCase();
            } else {
                bts = bts.toUpperCase();
            }
            rs += bts;
        }
        return rs;
    }
}
