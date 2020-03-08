package com.bigbaldy.poker.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

import java.nio.charset.Charset;
import java.util.Base64;

public class StringUtil {

    private static final Logger logger = LoggerFactory.getLogger(StringUtil.class);

    private static final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A',
            'B', 'C',
            'D', 'E', 'F'};

    public static String getRandomToken() {
        return "should be implemented still";
    }

    public static boolean isEmpty(@Nullable String str) {
        return str == null || "".equals(str.trim());
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static String toHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] fromHex(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public static String base64Encode(String str) {
        return base64Encode(getBytes(str));
    }

    public static String base64Encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String base64Decode(String str) {
        byte[] bytes = Base64.getDecoder().decode(str);
        return fromBytes(bytes);
    }

    public static int codePointsLength(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); ) {
            int codePoint = s.codePointAt(i);
            i += Character.charCount(codePoint);
            ++n;
        }
        return n;
    }

    public static byte[] getBytes(String string) {
        return string.getBytes(Charset.forName("UTF-8"));
    }

    public static String fromBytes(byte[] bytes) {
        return new String(bytes, Charset.forName("UTF-8"));
    }
}
