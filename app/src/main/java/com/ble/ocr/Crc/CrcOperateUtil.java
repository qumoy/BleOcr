/*
 * Copyright 2010 Beijing Xinwei, Inc. All rights reserved.
 *
 * History:
 * ------------------------------------------------------------------------------
 * Date    	|  Who  		|  What
 * 2013-2-1	| chenlong 	| 	create the file
 */

package com.ble.ocr.Crc;

import android.util.Log;


import com.ble.ocr.HexUtil;

import java.util.Arrays;

/**
 * CRC数组处理
 *
 * <p>
 * 类详细描述
 * </p>
 *
 * @author chenlong
 */

public class CrcOperateUtil {
    /**
     * 为Byte数组添加两位CRC校验
     *
     * @param buf
     * @return
     */
    public static byte[] setParamCRC(byte[] header, byte[] buf) {
        int MASK = 0x0001, CRCSEED = 0x0810;
        int remain = 0;

        byte val;
        for (int i = 0; i < buf.length; i++) {
            val = buf[i];
            for (int j = 0; j < 8; j++) {
                if (((val ^ remain) & MASK) != 0) {
                    remain ^= CRCSEED;
                    remain >>= 1;
                    remain |= 0x8000;
                } else {
                    remain >>= 1;
                }
                val >>= 1;
            }
        }

        byte[] crcByte = new byte[2];
        crcByte[0] = (byte) ((remain >> 8) & 0xff);
        crcByte[1] = (byte) (remain & 0xff);

        // 将新生成的byte数组添加到原数据结尾并返回
        return concatAll(header, buf, crcByte);
    }

    /**
     * 为Byte数组添加两位CRC校验
     *
     * @param buf
     * @return
     */
    public static byte[] setParamCRC(byte[] buf) {
        int MASK = 0x0001, CRCSEED = 0x0810;
        int remain = 0;

        byte val;
        for (int i = 0; i < buf.length; i++) {
            val = buf[i];
            for (int j = 0; j < 8; j++) {
                if (((val ^ remain) & MASK) != 0) {
                    remain ^= CRCSEED;
                    remain >>= 1;
                    remain |= 0x8000;
                } else {
                    remain >>= 1;
                }
                val >>= 1;
            }
        }

        byte[] crcByte = new byte[2];
        crcByte[0] = (byte) ((remain >> 8) & 0xff);
        crcByte[1] = (byte) (remain & 0xff);

        // 将新生成的byte数组添加到原数据结尾并返回
        return concatAll(buf, crcByte);
    }

    /**
     * 为Byte数组添加两位CRC校验
     *
     * @param buf
     * @return
     */
    public static byte[] setParamCRC(byte[] header, byte[] buf, byte[] tail) {
        int MASK = 0x0001, CRCSEED = 0x0810;
        int remain = 0;

        byte val;
        for (int i = 0; i < buf.length; i++) {
            val = buf[i];
            for (int j = 0; j < 8; j++) {
                if (((val ^ remain) & MASK) != 0) {
                    remain ^= CRCSEED;
                    remain >>= 1;
                    remain |= 0x8000;
                } else {
                    remain >>= 1;
                }
                val >>= 1;
            }
        }

        byte[] crcByte = new byte[2];
        crcByte[0] = (byte) ((remain >> 8) & 0xff);
        crcByte[1] = (byte) (remain & 0xff);

        // 将新生成的byte数组添加到原数据结尾并返回
        return concatAll(header, buf, crcByte);
    }

    static final char TABLE1021[] = { /* CRC1021余式表 */
            0x0000, 0x1021, 0x2042, 0x3063, 0x4084, 0x50a5, 0x60c6, 0x70e7,
            0x8108, 0x9129, 0xa14a, 0xb16b, 0xc18c, 0xd1ad, 0xe1ce, 0xf1ef,
            0x1231, 0x0210, 0x3273, 0x2252, 0x52b5, 0x4294, 0x72f7, 0x62d6,
            0x9339, 0x8318, 0xb37b, 0xa35a, 0xd3bd, 0xc39c, 0xf3ff, 0xe3de,
            0x2462, 0x3443, 0x0420, 0x1401, 0x64e6, 0x74c7, 0x44a4, 0x5485,
            0xa56a, 0xb54b, 0x8528, 0x9509, 0xe5ee, 0xf5cf, 0xc5ac, 0xd58d,
            0x3653, 0x2672, 0x1611, 0x0630, 0x76d7, 0x66f6, 0x5695, 0x46b4,
            0xb75b, 0xa77a, 0x9719, 0x8738, 0xf7df, 0xe7fe, 0xd79d, 0xc7bc,
            0x48c4, 0x58e5, 0x6886, 0x78a7, 0x0840, 0x1861, 0x2802, 0x3823,
            0xc9cc, 0xd9ed, 0xe98e, 0xf9af, 0x8948, 0x9969, 0xa90a, 0xb92b,
            0x5af5, 0x4ad4, 0x7ab7, 0x6a96, 0x1a71, 0x0a50, 0x3a33, 0x2a12,
            0xdbfd, 0xcbdc, 0xfbbf, 0xeb9e, 0x9b79, 0x8b58, 0xbb3b, 0xab1a,
            0x6ca6, 0x7c87, 0x4ce4, 0x5cc5, 0x2c22, 0x3c03, 0x0c60, 0x1c41,
            0xedae, 0xfd8f, 0xcdec, 0xddcd, 0xad2a, 0xbd0b, 0x8d68, 0x9d49,
            0x7e97, 0x6eb6, 0x5ed5, 0x4ef4, 0x3e13, 0x2e32, 0x1e51, 0x0e70,
            0xff9f, 0xefbe, 0xdfdd, 0xcffc, 0xbf1b, 0xaf3a, 0x9f59, 0x8f78,
            0x9188, 0x81a9, 0xb1ca, 0xa1eb, 0xd10c, 0xc12d, 0xf14e, 0xe16f,
            0x1080, 0x00a1, 0x30c2, 0x20e3, 0x5004, 0x4025, 0x7046, 0x6067,
            0x83b9, 0x9398, 0xa3fb, 0xb3da, 0xc33d, 0xd31c, 0xe37f, 0xf35e,
            0x02b1, 0x1290, 0x22f3, 0x32d2, 0x4235, 0x5214, 0x6277, 0x7256,
            0xb5ea, 0xa5cb, 0x95a8, 0x8589, 0xf56e, 0xe54f, 0xd52c, 0xc50d,
            0x34e2, 0x24c3, 0x14a0, 0x0481, 0x7466, 0x6447, 0x5424, 0x4405,
            0xa7db, 0xb7fa, 0x8799, 0x97b8, 0xe75f, 0xf77e, 0xc71d, 0xd73c,
            0x26d3, 0x36f2, 0x0691, 0x16b0, 0x6657, 0x7676, 0x4615, 0x5634,
            0xd94c, 0xc96d, 0xf90e, 0xe92f, 0x99c8, 0x89e9, 0xb98a, 0xa9ab,
            0x5844, 0x4865, 0x7806, 0x6827, 0x18c0, 0x08e1, 0x3882, 0x28a3,
            0xcb7d, 0xdb5c, 0xeb3f, 0xfb1e, 0x8bf9, 0x9bd8, 0xabbb, 0xbb9a,
            0x4a75, 0x5a54, 0x6a37, 0x7a16, 0x0af1, 0x1ad0, 0x2ab3, 0x3a92,
            0xfd2e, 0xed0f, 0xdd6c, 0xcd4d, 0xbdaa, 0xad8b, 0x9de8, 0x8dc9,
            0x7c26, 0x6c07, 0x5c64, 0x4c45, 0x3ca2, 0x2c83, 0x1ce0, 0x0cc1,
            0xef1f, 0xff3e, 0xcf5d, 0xdf7c, 0xaf9b, 0xbfba, 0x8fd9, 0x9ff8,
            0x6e17, 0x7e36, 0x4e55, 0x5e74, 0x2e93, 0x3eb2, 0x0ed1, 0x1ef0,};

    public static char getCRC1021(byte b[]) {
        int len = b.length;
        char crc = 0;
        byte hb = 0;
        int j = 0;
        int index;
        while (len-- != 0) {
            hb = (byte) (crc / 256); //以8位二进制数的形式暂存CRC的高8位
            index = ((hb ^ b[j]) & 0xff); //求得所查索引下标
            crc <<= 8; // 左移8位，相当于CRC的低8位乘以
            crc ^= (TABLE1021[index]); // 高8位和当前字节相加后再查表求CRC ，再加上以前的CRC
            j++;
        }
        return (crc);
    }


    /**
     * 根据起始和结束下标截取byte数组
     *
     * @param bytes
     * @param start
     * @param end
     * @return
     */
    private static byte[] getBytesByindex(byte[] bytes, int start, int end) {
        byte[] returnBytes = new byte[end - start + 1];
        for (int i = 0; i < returnBytes.length; i++) {
            returnBytes[i] = bytes[start + i];
        }
        return returnBytes;
    }

    /**
     * 对buf中offset以前crcLen长度的字节作crc校验，返回校验结果
     *
     * @param buf    byte[]
     * @param offset int
     * @param crcLen int　crc校验的长度
     * @return int　crc结果
     */
    private static int calcCRC(byte[] buf, int offset, int crcLen) {
        Log.e("isPassCRC", buf.length + "  length ");
        int MASK = 0x0001, CRCSEED = 0x0810;
        int start = offset;
        int end = offset + crcLen;
        int remain = 0;

        byte val;
        for (int i = start; i < end; i++) {
            val = buf[i];
            for (int j = 0; j < 8; j++) {
                if (((val ^ remain) & MASK) != 0) {
                    remain ^= CRCSEED;
                    remain >>= 1;
                    remain |= 0x8000;
                } else {
                    remain >>= 1;
                }
                val >>= 1;
            }
        }
        return remain;
    }

    /***
     * CRC校验是否通过
     *
     * @param srcByte
     * @param length
     * @return
     */
    public static boolean isPassCRC(byte[] srcByte, int length) {
        // 取出除crc校验位的其他数组，进行计算，得到CRC校验结果
        int calcCRC = calcCRC(srcByte, 0, srcByte.length - length);

        // 取出CRC校验位，进行计算
        int receive = toInt(getBytesByindex(srcByte, srcByte.length - length, srcByte.length - 1));
        Log.e("isPassCRC", receive + "   ");

        // 比较
        return calcCRC == receive;
    }

    /***
     * CRC校验是否通过
     *
     * @param srcByte
     * @param
     * @return
     */
    public static boolean isPassCRC(byte[] srcByte, int headerLength, int crcLength) {
        // 取出除crc校验位的其他数组，进行计算，得到CRC校验结果
        String s = HexUtil.byte2hex(srcByte, headerLength, srcByte.length - crcLength - 1);
        byte[] bytes = HexUtil.hexStringToBytes(s);
        int calcCRC = getCRC1021(bytes);

        // 取出CRC校验位，进行计算
        int receive = toInt(getBytesByindex(srcByte, srcByte.length - crcLength, srcByte.length - 1));

        // 比较
        return calcCRC == receive;
    }

    /***
     * CRC校验是否通过
     *
     * @param srcByte
     * @param
     * @return
     */
    public static boolean isPassCRC(byte[] srcByte, int headerLength, int tailLength, int crcLength) {
        // 取出除crc校验位的其他数组，进行计算，得到CRC校验结果
        int calcCRC = calcCRC(srcByte, headerLength, srcByte.length - crcLength - tailLength);

        // 取出CRC校验位，进行计算
        int receive = toInt(getBytesByindex(srcByte, srcByte.length - crcLength, srcByte.length - 1));

        // 比较
        return calcCRC == receive;
    }

    /**
     * 多个数组合并
     *
     * @param first
     * @param rest
     * @return
     */
    public static byte[] concatAll(byte[] first, byte[]... rest) {
        int totalLength = first.length;
        for (byte[] array : rest) {
            totalLength += array.length;
        }
        byte[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (byte[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    /**
     * Byte转换为Int
     *
     * @param b
     * @return
     */
    public static int toInt(byte[] b) {
        return toInt(b, 0, 4);
    }

    /**
     * Byte转换为Int
     *
     * @param b
     * @param off
     * @param len
     * @return
     */
    public static int toInt(byte[] b, int off, int len) {
        int st = 0;
        if (off < 0)
            off = 0;
        if (len > 4)
            len = 4;
        for (int i = 0; i < len && (i + off) < b.length; i++) {
            st <<= 8;
            st += (int) b[i + off] & 0xff;
        }
        return st;
    }

    public static byte[] setCRC1021(byte[] b) {
        int len = b.length;
        char crc = 0;
        byte hb = 0;
        int j = 0;
        int index;
        while (len-- != 0) {
            hb = (byte) (crc / 256); //以8位二进制数的形式暂存CRC的高8位
            index = ((hb ^ b[j]) & 0xff); //求得所查索引下标
            crc <<= 8; // 左移8位，相当于CRC的低8位乘以
            crc ^= (TABLE1021[index]); // 高8位和当前字节相加后再查表求CRC ，再加上以前的CRC
            j++;
        }
        String str = Integer.toHexString(crc).toUpperCase();
        String crcStr = String.format("%04x", Integer.parseInt(str, 16));
//        Log.e("PdaBaseActivity", "setCRC1021: " + crcStr);
        byte[] bytes = HexUtil.hexStringToBytes(crcStr);
        return bytes;
    }

    public static byte[] setCRC(byte[] b) {
        int len = b.length;
        char crc = 0;
        byte hb = 0;
        int j = 0;
        int index;
        for (int i = 0; i < len; i++) {
            hb = (byte) (crc >> 8); //以8位二进制数的形式暂存CRC的高8位
            index = ((hb ^ b[j]) & 0xff); //求得所查索引下标
            crc <<= 8; // 左移8位，相当于CRC的低8位乘以
            crc ^= (TABLE1021[index]); // 高8位和当前字节相加后再查表求CRC ，再加上以前的CRC
            j++;
        }
        String str = Integer.toHexString(crc).toUpperCase();
        String crcStr = String.format("%04x", Integer.parseInt(str, 16));
        byte[] bytes = HexUtil.hexStringToBytes(crcStr);
        return bytes;
    }

    public static byte[] setFileCRC(byte[] buffer) {
//        char wCRC = 0xFFFF;
//        char reg_crc = (char) (((wCRC & 0x00FF) << 8) | (wCRC >> 8));
//        int length = buffer.length;
//        int l = 0;
//        while (length-- != 0) {
//            reg_crc ^= buffer[l];
//            l++;
//            for (int i = 0; i < 8; i++) {
//                if ((reg_crc & 0x01) == 0x01) {
//                    reg_crc = (char) ((reg_crc >> 1) ^ 0xA001);
//                } else {
//                    reg_crc = (char) (reg_crc >> 1);
//                }
//            }
//            char LowByte = (char) (reg_crc >> 8);
//            char HighByte = (char) (reg_crc & 0x00FF);
//            reg_crc = (char) ((HighByte << 8) | LowByte);
//        }
        int reg_crc = 0xffff;
        int length = buffer.length;
        for (int i = 0; i < length; i++) {
            reg_crc ^= ((int) buffer[i] & 0xff);
            for (int j = 0; j < 8; j++) {
                if ((reg_crc & 0x01) != 0) {
                    reg_crc = (reg_crc >> 1) ^ 0xa001;
                } else {
                    reg_crc = reg_crc >> 1;
                }
            }
        }
        String str = Integer.toHexString(reg_crc).toUpperCase();
        String crcStr = String.format("%04x", Integer.parseInt(str, 16));

        return HexUtil.hexStringToBytes(crcStr);
    }
}
