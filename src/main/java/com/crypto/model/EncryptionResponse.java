package com.crypto.model;

/**
 * Response object for card number encryption.
 */
public class EncryptionResponse {

    private boolean success;
    private String encryptedDataHex;
    private String errorMessage;

    public EncryptionResponse() {
    }

    public EncryptionResponse(boolean success, String encryptedDataHex, String errorMessage) {
        this.success = success;
        this.encryptedDataHex = encryptedDataHex;
        this.errorMessage = errorMessage;
    }

    public static EncryptionResponse success(String encryptedDataHex) {
        return new EncryptionResponse(true, encryptedDataHex, null);
    }

    public static EncryptionResponse failure(String errorMessage) {
        return new EncryptionResponse(false, null, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getEncryptedDataHex() {
        return encryptedDataHex;
    }

    public void setEncryptedDataHex(String encryptedDataHex) {
        this.encryptedDataHex = encryptedDataHex;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "EncryptionResponse{" +
                "success=" + success +
                ", encryptedDataHex='"
                + (encryptedDataHex != null
                        ? encryptedDataHex.substring(0, Math.min(40, encryptedDataHex.length())) + "..."
                        : "null")
                + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
