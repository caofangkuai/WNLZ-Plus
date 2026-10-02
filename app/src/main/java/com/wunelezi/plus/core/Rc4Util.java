package com.wunelezi.plus.core;

import java.io.UnsupportedEncodingException;

/**
 * RC4 used by the remote card-key service.
 */
public final class Rc4Util {

    private Rc4Util() {
    }

    public static String encryRC4String(String data, String key, String chartSet) throws UnsupportedEncodingException {
        if (data == null || key == null) return null;
        return bytesToHex(encryRC4Byte(data, key, chartSet));
    }

    public static byte[] encryRC4Byte(String data, String key, String chartSet) throws UnsupportedEncodingException {
        if (data == null || key == null) return null;
        byte[] bData = (chartSet == null || chartSet.isEmpty()) ? data.getBytes() : data.getBytes(chartSet);
        return rc4(bData, key);
    }

    public static String decryRC4(String data, String key, String chartSet) throws UnsupportedEncodingException {
        if (data == null || key == null) return null;
        return new String(rc4(hexToByte(data), key), chartSet);
    }

    private static byte[] initKey(String aKey) {
        byte[] bkey = aKey.getBytes();
        byte[] state = new byte[256];
        for (int i = 0; i < 256; i++) state[i] = (byte) i;
        int index1 = 0;
        int index2 = 0;
        if (bkey.length == 0) return null;
        for (int i = 0; i < 256; i++) {
            index2 = ((bkey[index1] & 0xff) + (state[i] & 0xff) + index2) & 0xff;
            byte tmp = state[i];
            state[i] = state[index2];
            state[index2] = tmp;
            index1 = (index1 + 1) % bkey.length;
        }
        return state;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            String hex = Integer.toHexString(aByte & 0xFF);
            if (hex.length() < 2) sb.append(0);
            sb.append(hex);
        }
        return sb.toString();
    }

    public static byte[] hexToByte(String inHex) {
        int hexlen = inHex.length();
        byte[] result;
        if (hexlen % 2 == 1) {
            hexlen++;
            result = new byte[(hexlen / 2)];
            inHex = "0" + inHex;
        } else {
            result = new byte[(hexlen / 2)];
        }
        int j = 0;
        for (int i = 0; i < hexlen; i += 2) {
            result[j] = (byte) Integer.parseInt(inHex.substring(i, i + 2), 16);
            j++;
        }
        return result;
    }

    private static byte[] rc4(byte[] input, String mKkey) {
        int x = 0;
        int y = 0;
        byte[] key = initKey(mKkey);
        int xorIndex;
        byte[] result = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            x = (x + 1) & 0xff;
            if (key == null) return result;
            y = ((key[x] & 0xff) + y) & 0xff;
            byte tmp = key[x];
            key[x] = key[y];
            key[y] = tmp;
            xorIndex = ((key[x] & 0xff) + (key[y] & 0xff)) & 0xff;
            result[i] = (byte) (input[i] ^ key[xorIndex]);
        }
        return result;
    }
}
