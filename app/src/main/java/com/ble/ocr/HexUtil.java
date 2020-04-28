package com.ble.ocr;

import java.io.UnsupportedEncodingException;

public class HexUtil {

    public static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    public static String byte2hex(byte[] src, int offset, int len) {
        String hex = "";
        for (int i = 0; i < len; i++) {
            hex += Integer.toHexString((src[offset + i] & 0x000000FF) | 0xFFFFFF00).substring(6)
                    .toUpperCase();
        }
        return hex;
    }

    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }

        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));

        }
        return d;
    }

    public static String convertStringToHex(String str) {

        char[] chars = str.toCharArray();

        StringBuffer hex = new StringBuffer();
        for (int i = 0; i < chars.length; i++) {
            hex.append(Integer.toHexString((int) chars[i]));
        }

        return hex.toString();
    }

    public static byte[] stringToByte(String string) {
        return hexStringToBytes(convertStringToHex(string));
    }

    /**
     * 两个 byte数组融合
     */
    public static byte[] byteMerge(byte[] bt1, byte[] bt2) {
        byte[] bt3 = new byte[bt1.length + bt2.length];
        System.arraycopy(bt1, 0, bt3, 0, bt1.length);
        System.arraycopy(bt2, 0, bt3, bt1.length, bt2.length);
        return bt3;
    }

    /**
     * 16进制直接转换成为字符串(无需Unicode解码)
     *
     * @param hexStr
     * @return
     */
    public static String hexStr2Str(String hexStr) {
        String str = "0123456789ABCDEF";
        char[] hexs = hexStr.toCharArray();
        byte[] bytes = new byte[hexStr.length() / 2];
        int n;
        for (int i = 0; i < bytes.length; i++) {
            n = str.indexOf(hexs[2 * i]) * 16;
            n += str.indexOf(hexs[2 * i + 1]);
            bytes[i] = (byte) (n & 0xff);
        }
        return new String(bytes);
    }

    /**
     * 十进制转十六进制
     */
    public static String intToHex(int n) {
        //StringBuffer s = new StringBuffer();
        StringBuilder sb = new StringBuilder(8);
        String a;
        char[] b = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        while (n != 0) {
            sb = sb.append(b[n % 16]);
            n = n / 16;
        }
        a = sb.reverse().toString();
        return a;
    }

    /**
     * UTF-8编码 转换为对应的 汉字
     *
     * URLEncoder.encode("上海", "UTF-8") ---> %E4%B8%8A%E6%B5%B7
     * URLDecoder.decode("%E4%B8%8A%E6%B5%B7", "UTF-8") --> 上 海
     *
     * convertUTF8ToString("E4B88AE6B5B7")
     * E4B88AE6B5B7 --> 上海
     *
     * @param s
     * @return
     */
    public static String convertUTF8ToString(String s) {
        if (s == null || s.equals("")) {
            return null;
        }

        try {
            s = s.toUpperCase();

            int total = s.length() / 2;
            int pos = 0;

            byte[] buffer = new byte[total];
            for (int i = 0; i < total; i++) {

                int start = i * 2;

                buffer[i] = (byte) Integer.parseInt(
                        s.substring(start, start + 2), 16);
                pos++;
            }

            return new String(buffer, 0, pos, "UTF-8");

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return s;
    }

    /**
     * 将文件名中的汉字转为UTF8编码的串,以便下载时能正确显示另存的文件名.
     *
     * @param s 原串
     * @return
     */
    public static String convertStringToUTF8(String s) {
        if (s == null || s.equals("")) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        try {
            char c;
            for (int i = 0; i < s.length(); i++) {
                c = s.charAt(i);
                if (c >= 0 && c <= 255) {
                    sb.append(c);
                } else {
                    byte[] b;

                    b = Character.toString(c).getBytes("utf-8");

                    for (int j = 0; j < b.length; j++) {
                        int k = b[j];
                        if (k < 0)
                            k += 256;
                        sb.append(Integer.toHexString(k).toUpperCase());
                        // sb.append("%" +Integer.toHexString(k).toUpperCase());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
        return sb.toString();
    }
    public static byte[] StringToByteArray(String hex) throws Exception {
        if (hex == null) {
            throw new Exception("The binary key is empty");
        }
        int len = hex.length();
        if (len % 2 == 1) {
            throw new Exception(
                    "The binary key cannot have an odd number of digits");
        }

        byte[] ba = new byte[len / 2];
        for (int i = 0; i < ba.length; i++)
            ba[i] = (byte) Integer
                    .parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        return ba;
    }
}
