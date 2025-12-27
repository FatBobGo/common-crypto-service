package com.crypto.jni;

import com.crypto.model.EncryptionRequest;
import com.crypto.model.EncryptionResponse;
import com.crypto.service.CryptoService;
import com.crypto.service.impl.CryptoServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JNI-friendly wrapper for the crypto service.
 * This class provides static methods that can be easily called from RPGLE
 * programs
 * via JNI, avoiding memory management issues and simplifying the integration.
 * 
 * <p>
 * Design principles:
 * <ul>
 * <li>Static methods - no object lifecycle management needed from RPGLE</li>
 * <li>Simple parameters - only strings, no complex objects</li>
 * <li>Error codes - return codes instead of throwing exceptions</li>
 * <li>Thread-safe - each call creates its own service instance</li>
 * </ul>
 * 
 * <p>
 * Usage from RPGLE:
 * 
 * <pre>
 * D result         S               O   CLASS(*JAVA:'com.crypto.jni.JniEncryptionResult')
 * D cardNumber     S            100A   VARYING
 * D publicKeyHex   S          10000A   VARYING
 * D errorCode      S             10I 0
 * D encryptedData  S          32767A   VARYING
 * 
 * cardNumber = '4532123456789012';
 * publicKeyHex = '...hex string...';
 * 
 * result = JniCryptoWrapper_encryptCardNumber(cardNumber: publicKeyHex);
 * errorCode = JniEncryptionResult_getErrorCode(result);
 * 
 * if errorCode = 0;
 *   encryptedData = JniEncryptionResult_getEncryptedDataHex(result);
 *   // Process encrypted data
 * else;
 *   // Handle error
 * endif;
 * </pre>
 */
public final class JniCryptoWrapper {

    private static final Logger logger = LoggerFactory.getLogger(JniCryptoWrapper.class);

    // Private constructor to prevent instantiation
    private JniCryptoWrapper() {
        throw new AssertionError("Cannot instantiate JniCryptoWrapper");
    }

    /**
     * Encrypts a card number using the provided RSA public key.
     * This is the main entry point for RPGLE programs.
     * 
     * <p>
     * The method is thread-safe and creates a new CryptoService instance
     * for each call to avoid any state-related issues or memory leaks.
     * 
     * @param cardNumber      the clear card number to encrypt (e.g.,
     *                        "4532123456789012")
     * @param rsaPublicKeyHex the RSA public key in hex format (X.509 encoded)
     * @return JniEncryptionResult containing encrypted data or error information
     * 
     * @see JniEncryptionResult
     * @see JniErrorCodes
     */
    public static JniEncryptionResult encryptCardNumber(String cardNumber, String rsaPublicKeyHex) {
        logger.debug("JNI encryption request received");

        try {
            // Validate inputs
            if (cardNumber == null || cardNumber.trim().isEmpty()) {
                logger.warn("Invalid input: card number is null or empty");
                return JniEncryptionResult.failure(
                        JniErrorCodes.INVALID_INPUT,
                        "Card number cannot be null or empty");
            }

            if (rsaPublicKeyHex == null || rsaPublicKeyHex.trim().isEmpty()) {
                logger.warn("Invalid input: RSA public key is null or empty");
                return JniEncryptionResult.failure(
                        JniErrorCodes.INVALID_PUBLIC_KEY,
                        "RSA public key cannot be null or empty");
            }

            // Create encryption request
            EncryptionRequest request = new EncryptionRequest(rsaPublicKeyHex, cardNumber);

            // Create a new service instance for thread safety
            CryptoService cryptoService = new CryptoServiceImpl();

            // Perform encryption
            EncryptionResponse response = cryptoService.encryptCardNumber(request);

            // Convert response to JNI result
            if (response.isSuccess()) {
                logger.info("Encryption completed successfully via JNI");
                return JniEncryptionResult.success(response.getEncryptedDataHex());
            } else {
                logger.error("Encryption failed: {}", response.getErrorMessage());
                return JniEncryptionResult.failure(
                        JniErrorCodes.ENCRYPTION_FAILED,
                        response.getErrorMessage());
            }

        } catch (IllegalArgumentException e) {
            // Likely invalid hex format in public key
            logger.error("Invalid public key format", e);
            return JniEncryptionResult.failure(
                    JniErrorCodes.INVALID_PUBLIC_KEY,
                    "Invalid RSA public key format: " + e.getMessage());
        } catch (Exception e) {
            // Catch-all for unexpected errors
            logger.error("Unexpected error during JNI encryption", e);
            return JniEncryptionResult.failure(
                    JniErrorCodes.UNEXPECTED_ERROR,
                    "Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Convenience method for testing - encrypts with a simple success/failure
     * message.
     * This can be used for initial JNI connectivity testing from RPGLE.
     * 
     * @param testInput any test string
     * @return "SUCCESS: " + testInput if not null/empty, otherwise error result
     */
    public static JniEncryptionResult testConnection(String testInput) {
        logger.info("JNI test connection called with input: {}", testInput);

        if (testInput == null || testInput.trim().isEmpty()) {
            return JniEncryptionResult.failure(
                    JniErrorCodes.INVALID_INPUT,
                    "Test input cannot be null or empty");
        }

        return JniEncryptionResult.success("SUCCESS: " + testInput);
    }
}
