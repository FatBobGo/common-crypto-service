package com.crypto.jni;

/**
 * Result object for JNI encryption operations.
 * This class is designed to be easily consumed by RPGLE programs via JNI.
 */
public class JniEncryptionResult {

    private final int errorCode;
    private final String encryptedDataHex;
    private final String errorMessage;

    /**
     * Creates a new encryption result.
     * 
     * @param errorCode        the error code (0 = success, see JniErrorCodes)
     * @param encryptedDataHex the encrypted data in hex format (null on error)
     * @param errorMessage     human-readable error message (null on success)
     */
    public JniEncryptionResult(int errorCode, String encryptedDataHex, String errorMessage) {
        this.errorCode = errorCode;
        this.encryptedDataHex = encryptedDataHex;
        this.errorMessage = errorMessage;
    }

    /**
     * Creates a successful result.
     * 
     * @param encryptedDataHex the encrypted data in hex format
     * @return a success result
     */
    public static JniEncryptionResult success(String encryptedDataHex) {
        return new JniEncryptionResult(JniErrorCodes.SUCCESS, encryptedDataHex, null);
    }

    /**
     * Creates a failure result.
     * 
     * @param errorCode    the error code
     * @param errorMessage the error message
     * @return a failure result
     */
    public static JniEncryptionResult failure(int errorCode, String errorMessage) {
        return new JniEncryptionResult(errorCode, null, errorMessage);
    }

    /**
     * Gets the error code.
     * 
     * @return 0 for success, non-zero for error (see JniErrorCodes)
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Gets the encrypted data in hex format.
     * 
     * @return encrypted data hex string, or null if operation failed
     */
    public String getEncryptedDataHex() {
        return encryptedDataHex;
    }

    /**
     * Gets the error message.
     * 
     * @return error message, or null if operation succeeded
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Checks if the operation was successful.
     * 
     * @return true if errorCode is 0, false otherwise
     */
    public boolean isSuccess() {
        return errorCode == JniErrorCodes.SUCCESS;
    }

    @Override
    public String toString() {
        return "JniEncryptionResult{" +
                "errorCode=" + errorCode +
                ", encryptedDataHex='" +
                (encryptedDataHex != null
                        ? encryptedDataHex.substring(0, Math.min(40, encryptedDataHex.length())) + "..."
                        : "null")
                +
                "', errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
