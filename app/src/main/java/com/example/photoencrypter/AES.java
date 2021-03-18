package com.example.photoencrypter;

public class AES {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    public static final int ENCRYPT = 0;
    public static final int DECRYPT = 1;

    public static native byte[] encrypt_byte_array(byte[] data, byte[] key);
    public static native byte[] decrypt_byte_array(byte[] data, byte[] key);

    public static byte[] hexStr2Bytes(String hexStr) {
        if (hexStr.length() < 1)
            return null;
        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length() / 2; i++) {
            int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
            int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }

    public static String bytes2HexStr(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        StringBuilder buf = new StringBuilder();
        for (byte aData : data) {
            if (((int) aData & 0xff) < 0x10) {
                buf.append("0");
            }
            buf.append(Long.toHexString((int) aData & 0xff));
        }
        return buf.toString();
    }
}
