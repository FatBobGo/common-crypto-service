package com.crypto.util;

/**
 * Utility class for hexadecimal encoding and decoding.
 */
public final class HexUtil {

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private HexUtil() {
        throw new AssertionError("Cannot instantiate utility class");
    }

    /**
     * Converts a byte array to a hexadecimal string.
     *
     * @param bytes the byte array to convert
     * @return the hexadecimal string representation
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("Byte array cannot be null");
        }

        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_ARRAY[v >>> 4];
            hexChars[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Converts a hexadecimal string to a byte array.
     *
     * @param hex the hexadecimal string to convert
     * @return the byte array representation
     * @throws IllegalArgumentException if the hex string is invalid
     */
    public static byte[] hexToBytes(String hex) {
        if (hex == null) {
            throw new IllegalArgumentException("Hex string cannot be null");
        }

        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }

        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;
            int firstDigit = Character.digit(hex.charAt(index), 16);
            int secondDigit = Character.digit(hex.charAt(index + 1), 16);

            if (firstDigit == -1 || secondDigit == -1) {
                throw new IllegalArgumentException("Invalid hex character at position " + index);
            }

            bytes[i] = (byte) ((firstDigit << 4) + secondDigit);
        }
        return bytes;
    }
}
