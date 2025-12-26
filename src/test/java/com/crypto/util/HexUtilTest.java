package com.crypto.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HexUtil.
 */
class HexUtilTest {

    @Test
    void testBytesToHex_ValidInput() {
        byte[] bytes = { 0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF };
        String hex = HexUtil.bytesToHex(bytes);
        assertEquals("0123456789ABCDEF", hex);
    }

    @Test
    void testBytesToHex_EmptyArray() {
        byte[] bytes = {};
        String hex = HexUtil.bytesToHex(bytes);
        assertEquals("", hex);
    }

    @Test
    void testBytesToHex_NullInput() {
        assertThrows(IllegalArgumentException.class, () -> HexUtil.bytesToHex(null));
    }

    @Test
    void testHexToBytes_ValidInput() {
        String hex = "0123456789ABCDEF";
        byte[] bytes = HexUtil.hexToBytes(hex);
        assertArrayEquals(new byte[] { 0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF },
                bytes);
    }

    @Test
    void testHexToBytes_LowerCase() {
        String hex = "0123456789abcdef";
        byte[] bytes = HexUtil.hexToBytes(hex);
        assertArrayEquals(new byte[] { 0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF },
                bytes);
    }

    @Test
    void testHexToBytes_EmptyString() {
        String hex = "";
        byte[] bytes = HexUtil.hexToBytes(hex);
        assertArrayEquals(new byte[] {}, bytes);
    }

    @Test
    void testHexToBytes_NullInput() {
        assertThrows(IllegalArgumentException.class, () -> HexUtil.hexToBytes(null));
    }

    @Test
    void testHexToBytes_OddLength() {
        String hex = "123";
        assertThrows(IllegalArgumentException.class, () -> HexUtil.hexToBytes(hex));
    }

    @ParameterizedTest
    @ValueSource(strings = { "GG", "ZZ", "!@", "  " })
    void testHexToBytes_InvalidCharacters(String invalidHex) {
        assertThrows(IllegalArgumentException.class, () -> HexUtil.hexToBytes(invalidHex));
    }

    @Test
    void testRoundTrip() {
        byte[] original = { 0x12, 0x34, 0x56, 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0 };
        String hex = HexUtil.bytesToHex(original);
        byte[] decoded = HexUtil.hexToBytes(hex);
        assertArrayEquals(original, decoded);
    }
}
