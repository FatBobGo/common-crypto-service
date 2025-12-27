package com.crypto.jni;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeAll;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.HexFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JniCryptoWrapper.
 */
class JniCryptoWrapperTest {

    private static String validPublicKeyHex;
    private static final String VALID_CARD_NUMBER = "4532123456789012";

    @BeforeAll
    static void setUp() throws Exception {
        // Generate a valid RSA key pair for testing
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        KeyPair keyPair = keyPairGen.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        validPublicKeyHex = HexFormat.of().withUpperCase().formatHex(publicKey.getEncoded());
    }

    @Test
    @DisplayName("Should successfully encrypt card number with valid inputs")
    void testSuccessfulEncryption() {
        JniEncryptionResult result = JniCryptoWrapper.encryptCardNumber(
                VALID_CARD_NUMBER,
                validPublicKeyHex);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Encryption should succeed");
        assertEquals(JniErrorCodes.SUCCESS, result.getErrorCode(), "Error code should be SUCCESS");
        assertNotNull(result.getEncryptedDataHex(), "Encrypted data should not be null");
        assertNull(result.getErrorMessage(), "Error message should be null on success");
        assertTrue(result.getEncryptedDataHex().length() > 0, "Encrypted data should not be empty");
    }

    @Test
    @DisplayName("Should return error when card number is null")
    void testNullCardNumber() {
        JniEncryptionResult result = JniCryptoWrapper.encryptCardNumber(null, validPublicKeyHex);

        assertNotNull(result, "Result should not be null");
        assertFalse(result.isSuccess(), "Encryption should fail");
        assertEquals(JniErrorCodes.INVALID_INPUT, result.getErrorCode(),
                "Error code should be INVALID_INPUT");
        assertNull(result.getEncryptedDataHex(), "Encrypted data should be null on error");
        assertNotNull(result.getErrorMessage(), "Error message should not be null");
        assertTrue(result.getErrorMessage().contains("Card"),
                "Error message should mention Card");
    }

    @Test
    @DisplayName("Should return error when card number is empty")
    void testEmptyCardNumber() {
        JniEncryptionResult result = JniCryptoWrapper.encryptCardNumber("", validPublicKeyHex);

        assertNotNull(result, "Result should not be null");
        assertFalse(result.isSuccess(), "Encryption should fail");
        assertEquals(JniErrorCodes.INVALID_INPUT, result.getErrorCode(),
                "Error code should be INVALID_INPUT");
    }

    @Test
    @DisplayName("Should return error when card number is whitespace only")
    void testWhitespaceCardNumber() {
        JniEncryptionResult result = JniCryptoWrapper.encryptCardNumber("   ", validPublicKeyHex);

        assertNotNull(result, "Result should not be null");
        assertFalse(result.isSuccess(), "Encryption should fail");
        assertEquals(JniErrorCodes.INVALID_INPUT, result.getErrorCode(),
                "Error code should be INVALID_INPUT");
    }

    @Test
    @DisplayName("Should return error when public key is null")
    void testNullPublicKey() {
        JniEncryptionResult result = JniCryptoWrapper.encryptCardNumber(VALID_CARD_NUMBER, null);

        assertNotNull(result, "Result should not be null");
        assertFalse(result.isSuccess(), "Encryption should fail");
        assertEquals(JniErrorCodes.INVALID_PUBLIC_KEY, result.getErrorCode(),
                "Error code should be INVALID_PUBLIC_KEY");
        assertNull(result.getEncryptedDataHex(), "Encrypted data should be null on error");
        assertNotNull(result.getErrorMessage(), "Error message should not be null");
    }

    @Test
    @DisplayName("Should return error when public key is empty")
    void testEmptyPublicKey() {
        JniEncryptionResult result = JniCryptoWrapper.encryptCardNumber(VALID_CARD_NUMBER, "");

        assertNotNull(result, "Result should not be null");
        assertFalse(result.isSuccess(), "Encryption should fail");
        assertEquals(JniErrorCodes.INVALID_PUBLIC_KEY, result.getErrorCode(),
                "Error code should be INVALID_PUBLIC_KEY");
    }

    @Test
    @DisplayName("Should return error when public key is invalid hex")
    void testInvalidPublicKeyHex() {
        JniEncryptionResult result = JniCryptoWrapper.encryptCardNumber(
                VALID_CARD_NUMBER,
                "INVALID_HEX_STRING_XYZ");

        // Invalid hex format - the wrapper catches IllegalArgumentException and returns
        // INVALID_PUBLIC_KEY,
        // but if it gets past hex parsing, it returns ENCRYPTION_FAILED
        assertTrue(
                result.getErrorCode() == JniErrorCodes.INVALID_PUBLIC_KEY ||
                        result.getErrorCode() == JniErrorCodes.ENCRYPTION_FAILED,
                "Error code should be INVALID_PUBLIC_KEY or ENCRYPTION_FAILED");
    }

    @Test
    @DisplayName("Should return error when public key is malformed")
    void testMalformedPublicKey() {
        // Valid hex but not a valid RSA public key
        JniEncryptionResult result = JniCryptoWrapper.encryptCardNumber(
                VALID_CARD_NUMBER,
                "ABCDEF1234567890");

        assertNotNull(result, "Result should not be null");
        assertFalse(result.isSuccess(), "Encryption should fail");
        assertTrue(
                result.getErrorCode() == JniErrorCodes.INVALID_PUBLIC_KEY ||
                        result.getErrorCode() == JniErrorCodes.ENCRYPTION_FAILED,
                "Error code should be INVALID_PUBLIC_KEY or ENCRYPTION_FAILED");
    }

    @Test
    @DisplayName("Should handle different card number formats")
    void testDifferentCardNumberFormats() {
        String[] cardNumbers = {
                "4532123456789012", // Visa
                "5425233430109903", // Mastercard
                "374245455400126", // Amex (15 digits)
                "6011111111111117", // Discover
                "3530111333300000" // JCB
        };

        for (String cardNumber : cardNumbers) {
            JniEncryptionResult result = JniCryptoWrapper.encryptCardNumber(
                    cardNumber,
                    validPublicKeyHex);

            assertTrue(result.isSuccess(),
                    "Encryption should succeed for card number: " + cardNumber);
            assertNotNull(result.getEncryptedDataHex(),
                    "Encrypted data should not be null for card number: " + cardNumber);
        }
    }

    @Test
    @DisplayName("Should be thread-safe with concurrent calls")
    void testThreadSafety() throws InterruptedException {
        int threadCount = 10;
        int callsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount * callsPerThread);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < callsPerThread; j++) {
                    try {
                        JniEncryptionResult result = JniCryptoWrapper.encryptCardNumber(
                                VALID_CARD_NUMBER,
                                validPublicKeyHex);

                        if (result.isSuccess()) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "All threads should complete");
        executor.shutdown();

        assertEquals(threadCount * callsPerThread, successCount.get(),
                "All calls should succeed");
        assertEquals(0, failureCount.get(), "No calls should fail");
    }

    @Test
    @DisplayName("Should successfully test connection")
    void testConnectionSuccess() {
        JniEncryptionResult result = JniCryptoWrapper.testConnection("Hello iSeries");

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isSuccess(), "Test should succeed");
        assertEquals(JniErrorCodes.SUCCESS, result.getErrorCode(), "Error code should be SUCCESS");
        assertNotNull(result.getEncryptedDataHex(), "Result data should not be null");
        assertEquals("SUCCESS: Hello iSeries", result.getEncryptedDataHex(),
                "Result should contain test input");
    }

    @Test
    @DisplayName("Should fail test connection with null input")
    void testConnectionFailure() {
        JniEncryptionResult result = JniCryptoWrapper.testConnection(null);

        assertNotNull(result, "Result should not be null");
        assertFalse(result.isSuccess(), "Test should fail");
        assertEquals(JniErrorCodes.INVALID_INPUT, result.getErrorCode(),
                "Error code should be INVALID_INPUT");
    }

    @Test
    @DisplayName("Should produce different encrypted outputs for same input")
    void testRandomnessInEncryption() {
        JniEncryptionResult result1 = JniCryptoWrapper.encryptCardNumber(
                VALID_CARD_NUMBER,
                validPublicKeyHex);
        JniEncryptionResult result2 = JniCryptoWrapper.encryptCardNumber(
                VALID_CARD_NUMBER,
                validPublicKeyHex);

        assertTrue(result1.isSuccess(), "First encryption should succeed");
        assertTrue(result2.isSuccess(), "Second encryption should succeed");
        assertNotEquals(result1.getEncryptedDataHex(), result2.getEncryptedDataHex(),
                "Encrypted outputs should be different due to random IV");
    }
}
