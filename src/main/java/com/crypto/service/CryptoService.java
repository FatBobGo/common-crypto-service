package com.crypto.service;

import com.crypto.exception.CryptoException;
import com.crypto.model.EncryptionRequest;
import com.crypto.model.EncryptionResponse;

/**
 * Service interface for cryptographic operations.
 */
public interface CryptoService {

    /**
     * Encrypts a card number using AES encryption, then encrypts the AES key using
     * RSA public key.
     * 
     * Flow:
     * 1. Generate a random AES-256 key
     * 2. Encrypt the card number using AES-GCM
     * 3. Encrypt the AES key using RSA public key with OAEP and MGF1 padding
     * 4. Combine IV + encrypted card + encrypted AES key into hex format
     *
     * @param request the encryption request containing RSA public key and card
     *                number
     * @return the encryption response containing the encrypted data in hex format
     */
    EncryptionResponse encryptCardNumber(EncryptionRequest request);

    /**
     * Generates a random AES key.
     *
     * @return the generated AES key bytes
     * @throws CryptoException if key generation fails
     */
    byte[] generateAESKey() throws CryptoException;
}
