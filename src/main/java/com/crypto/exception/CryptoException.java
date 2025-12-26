package com.crypto.exception;

/**
 * Custom exception for cryptographic operations.
 */
public class CryptoException extends Exception {
    
    public CryptoException(String message) {
        super(message);
    }
    
    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }
}
