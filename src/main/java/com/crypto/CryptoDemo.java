package com.crypto;

import com.crypto.model.EncryptionRequest;
import com.crypto.model.EncryptionResponse;
import com.crypto.service.CryptoService;
import com.crypto.service.impl.CryptoServiceImpl;
import com.crypto.util.HexUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;

/**
 * Demo application showing the encryption flow.
 */
public class CryptoDemo {

    private static final Logger logger = LoggerFactory.getLogger(CryptoDemo.class);

    public static void main(String[] args) {
        try {
            logger.info("=== Crypto Service Demo ===");

            // Step 1: Generate RSA key pair (simulating API consumer)
            logger.info("Step 1: Generating RSA key pair...");
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(2048);
            KeyPair keyPair = keyPairGen.generateKeyPair();
            PublicKey publicKey = keyPair.getPublic();

            // Convert public key to hex (as API consumer would send)
            String publicKeyHex = HexUtil.bytesToHex(publicKey.getEncoded());
            logger.info("RSA Public Key (hex): {}...", publicKeyHex.substring(0, 60));

            // Step 2: Create encryption request
            String cardNumber = "4532123456789012";
            logger.info("Step 2: Card number to encrypt: {}****", cardNumber.substring(0, 4));

            EncryptionRequest request = new EncryptionRequest(publicKeyHex, cardNumber);

            // Step 3: Encrypt using the service
            logger.info("Step 3: Encrypting card number...");
            CryptoService cryptoService = new CryptoServiceImpl();
            EncryptionResponse response = cryptoService.encryptCardNumber(request);

            // Step 4: Display result
            if (response.isSuccess()) {
                logger.info("Step 4: Encryption successful!");
                logger.info("Encrypted data (hex): {}...",
                        response.getEncryptedDataHex().substring(0,
                                Math.min(80, response.getEncryptedDataHex().length())));
                logger.info("Total encrypted data length: {} hex characters", response.getEncryptedDataHex().length());
                logger.info("\n=== Encryption Flow Complete ===");
                logger.info("The encrypted data contains:");
                logger.info("  - IV (Initialization Vector)");
                logger.info("  - Encrypted card number (AES-GCM)");
                logger.info("  - Encrypted AES key (RSA-OAEP with MGF1)");
            } else {
                logger.error("Encryption failed: {}", response.getErrorMessage());
            }

        } catch (Exception e) {
            logger.error("Demo failed", e);
        }
    }
}
