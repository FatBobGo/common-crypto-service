package com.crypto.service.impl;

import com.crypto.constants.CryptoConstants;
import com.crypto.exception.CryptoException;
import com.crypto.model.EncryptionRequest;
import com.crypto.model.EncryptionResponse;
import com.crypto.service.CryptoService;
import com.crypto.util.HexUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;

/**
 * Implementation of the CryptoService interface.
 * Provides AES-GCM encryption for card numbers and RSA-OAEP with MGF1 for key
 * encryption.
 */
public class CryptoServiceImpl implements CryptoService {

    private static final Logger logger = LoggerFactory.getLogger(CryptoServiceImpl.class);
    private final SecureRandom secureRandom;

    static {
        // Add Bouncy Castle as a security provider
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
            logger.info("Bouncy Castle provider added");
        }
    }

    public CryptoServiceImpl() {
        this.secureRandom = new SecureRandom();
        logger.info("CryptoServiceImpl initialized");
    }

    @Override
    public EncryptionResponse encryptCardNumber(EncryptionRequest request) {
        try {
            logger.debug("Starting encryption process for request: {}", request);

            // Validate input
            validateRequest(request);

            // Step 1: Generate random AES key
            byte[] aesKey = generateAESKey();
            logger.debug("Generated AES key of size: {} bytes", aesKey.length);

            // Step 2: Encrypt card number with AES-GCM
            byte[] iv = generateIV();
            byte[] encryptedCard = encryptWithAES(request.getCardNumber(), aesKey, iv);
            logger.debug("Encrypted card number, size: {} bytes", encryptedCard.length);

            // Step 3: Encrypt AES key with RSA public key
            PublicKey rsaPublicKey = parseRSAPublicKey(request.getRsaPublicKeyHex());
            byte[] encryptedAESKey = encryptWithRSA(aesKey, rsaPublicKey);
            logger.debug("Encrypted AES key with RSA, size: {} bytes", encryptedAESKey.length);

            // Step 4: Combine IV + encrypted card + encrypted AES key
            byte[] combinedData = combineData(iv, encryptedCard, encryptedAESKey);
            String encryptedDataHex = HexUtil.bytesToHex(combinedData);

            logger.info("Encryption completed successfully, output size: {} hex chars", encryptedDataHex.length());
            return EncryptionResponse.success(encryptedDataHex);

        } catch (Exception e) {
            logger.error("Encryption failed", e);
            return EncryptionResponse.failure("Encryption failed: " + e.getMessage());
        }
    }

    @Override
    public byte[] generateAESKey() throws CryptoException {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(CryptoConstants.AES_ALGORITHM);
            keyGen.init(CryptoConstants.AES_KEY_SIZE, secureRandom);
            SecretKey secretKey = keyGen.generateKey();
            return secretKey.getEncoded();
        } catch (Exception e) {
            throw new CryptoException("Failed to generate AES key", e);
        }
    }

    /**
     * Generates a random initialization vector (IV) for AES-GCM.
     */
    private byte[] generateIV() {
        byte[] iv = new byte[CryptoConstants.GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        return iv;
    }

    /**
     * Encrypts data using AES-GCM.
     * 
     * Note: In GCM mode, cipher.doFinal() automatically appends the
     * authentication tag (16 bytes for 128-bit tag) to the ciphertext.
     * The returned byte array contains: [ciphertext][GCM tag]
     */
    private byte[] encryptWithAES(String plaintext, byte[] aesKey, byte[] iv) throws CryptoException {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(aesKey, CryptoConstants.AES_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(CryptoConstants.GCM_TAG_LENGTH, iv);

            Cipher cipher = Cipher.getInstance(CryptoConstants.AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            return cipher.doFinal(plaintextBytes);

        } catch (Exception e) {
            throw new CryptoException("Failed to encrypt with AES", e);
        }
    }

    /**
     * Encrypts the AES key using RSA with OAEP and MGF1 padding.
     */
    private byte[] encryptWithRSA(byte[] data, PublicKey publicKey) throws CryptoException {
        try {
            Cipher cipher = Cipher.getInstance(CryptoConstants.RSA_TRANSFORMATION, CryptoConstants.PROVIDER);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(data);

        } catch (Exception e) {
            throw new CryptoException("Failed to encrypt with RSA", e);
        }
    }

    /**
     * Parses the RSA public key from hex string.
     */
    private PublicKey parseRSAPublicKey(String publicKeyHex) throws CryptoException {
        try {
            byte[] publicKeyBytes = HexUtil.hexToBytes(publicKeyHex);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(CryptoConstants.RSA_ALGORITHM);
            return keyFactory.generatePublic(keySpec);

        } catch (Exception e) {
            throw new CryptoException("Failed to parse RSA public key", e);
        }
    }

    /**
     * Combines IV, encrypted card data, and encrypted AES key into a single byte
     * array.
     * 
     * Format: [IV_LENGTH(4 bytes)][IV][ENCRYPTED_CARD_LENGTH(4
     * bytes)][ENCRYPTED_CARD][ENCRYPTED_AES_KEY]
     * 
     * Note: The encryptedCard parameter already includes the GCM authentication tag
     * appended by cipher.doFinal() (16 bytes for 128-bit tag).
     */
    private byte[] combineData(byte[] iv, byte[] encryptedCard, byte[] encryptedAESKey) {
        ByteBuffer buffer = ByteBuffer.allocate(
                4 + iv.length +
                        4 + encryptedCard.length +
                        encryptedAESKey.length);

        buffer.putInt(iv.length);
        buffer.put(iv);
        buffer.putInt(encryptedCard.length);
        buffer.put(encryptedCard);
        buffer.put(encryptedAESKey);

        return buffer.array();
    }

    /**
     * Validates the encryption request.
     */
    private void validateRequest(EncryptionRequest request) throws CryptoException {
        if (request == null) {
            throw new CryptoException("Request cannot be null");
        }
        if (request.getCardNumber() == null || request.getCardNumber().trim().isEmpty()) {
            throw new CryptoException("Card number cannot be null or empty");
        }
        if (request.getRsaPublicKeyHex() == null || request.getRsaPublicKeyHex().trim().isEmpty()) {
            throw new CryptoException("RSA public key cannot be null or empty");
        }
    }
}
