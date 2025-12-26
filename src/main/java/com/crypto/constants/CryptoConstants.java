package com.crypto.constants;

/**
 * Constants for cryptographic operations.
 */
public final class CryptoConstants {
    
    // AES Configuration
    public static final String AES_ALGORITHM = "AES";
    public static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    public static final int AES_KEY_SIZE = 256; // bits
    public static final int GCM_IV_LENGTH = 12; // bytes (96 bits recommended for GCM)
    public static final int GCM_TAG_LENGTH = 128; // bits
    
    // RSA Configuration
    public static final String RSA_ALGORITHM = "RSA";
    public static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    
    // Encoding
    public static final String CHARSET = "UTF-8";
    
    // Security Provider
    public static final String PROVIDER = "BC"; // Bouncy Castle
    
    private CryptoConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }
}
