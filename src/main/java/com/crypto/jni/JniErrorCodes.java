package com.crypto.jni;

/**
 * Error codes for JNI crypto operations.
 * These codes are designed to be easily consumed by RPGLE programs.
 */
public final class JniErrorCodes {

    /**
     * Operation completed successfully.
     */
    public static final int SUCCESS = 0;

    /**
     * Invalid input - card number is null or empty.
     */
    public static final int INVALID_INPUT = 1;

    /**
     * Invalid RSA public key - null, empty, or malformed hex string.
     */
    public static final int INVALID_PUBLIC_KEY = 2;

    /**
     * Encryption operation failed.
     */
    public static final int ENCRYPTION_FAILED = 3;

    /**
     * Unexpected error occurred.
     */
    public static final int UNEXPECTED_ERROR = 4;

    // Private constructor to prevent instantiation
    private JniErrorCodes() {
        throw new AssertionError("Cannot instantiate JniErrorCodes");
    }

    /**
     * Gets a human-readable description of the error code.
     * 
     * @param errorCode the error code
     * @return description of the error
     */
    public static String getDescription(int errorCode) {
        switch (errorCode) {
            case SUCCESS:
                return "Operation completed successfully";
            case INVALID_INPUT:
                return "Invalid input - card number is null or empty";
            case INVALID_PUBLIC_KEY:
                return "Invalid RSA public key";
            case ENCRYPTION_FAILED:
                return "Encryption operation failed";
            case UNEXPECTED_ERROR:
                return "Unexpected error occurred";
            default:
                return "Unknown error code: " + errorCode;
        }
    }
}
