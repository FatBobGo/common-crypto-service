package com.crypto.model;

/**
 * Request object for card number encryption.
 */
public class EncryptionRequest {

    private String rsaPublicKeyHex;
    private String cardNumber;

    public EncryptionRequest() {
    }

    public EncryptionRequest(String rsaPublicKeyHex, String cardNumber) {
        this.rsaPublicKeyHex = rsaPublicKeyHex;
        this.cardNumber = cardNumber;
    }

    public String getRsaPublicKeyHex() {
        return rsaPublicKeyHex;
    }

    public void setRsaPublicKeyHex(String rsaPublicKeyHex) {
        this.rsaPublicKeyHex = rsaPublicKeyHex;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    @Override
    public String toString() {
        return "EncryptionRequest{" +
                "rsaPublicKeyHex='"
                + (rsaPublicKeyHex != null
                        ? rsaPublicKeyHex.substring(0, Math.min(20, rsaPublicKeyHex.length())) + "..."
                        : "null")
                + '\'' +
                ", cardNumber='****"
                + (cardNumber != null && cardNumber.length() >= 4 ? cardNumber.substring(cardNumber.length() - 4)
                        : "****")
                + '\'' +
                '}';
    }
}
