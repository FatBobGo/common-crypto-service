package com.crypto.integration;

import com.crypto.model.EncryptionRequest;
import com.crypto.model.EncryptionResponse;
import com.crypto.service.CryptoService;
import com.crypto.service.impl.CryptoServiceImpl;
import com.crypto.util.HexUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the complete encryption flow.
 */
class IntegrationTest {

    private CryptoService cryptoService;

    @BeforeEach
    void setUp() {
        cryptoService = new CryptoServiceImpl();
    }

    @Test
    void testCompleteAPIFlow() throws Exception {
        // Simulate API consumer generating RSA key pair
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        KeyPair consumerKeyPair = keyPairGen.generateKeyPair();

        // Consumer sends public key in hex format
        String publicKeyHex = HexUtil.bytesToHex(consumerKeyPair.getPublic().getEncoded());

        // Backend receives request with public key and card number
        String cardNumber = "4532123456789012";
        EncryptionRequest request = new EncryptionRequest(publicKeyHex, cardNumber);

        // Backend encrypts the card number
        EncryptionResponse response = cryptoService.encryptCardNumber(request);

        // Verify response
        assertTrue(response.isSuccess());
        assertNotNull(response.getEncryptedDataHex());
        assertNull(response.getErrorMessage());

        // Verify encrypted data is in hex format
        assertDoesNotThrow(() -> HexUtil.hexToBytes(response.getEncryptedDataHex()));

        // Verify encrypted data has reasonable size
        // Should contain: IV (12 bytes) + encrypted card (~28 bytes with GCM tag) +
        // encrypted AES key (256 bytes for 2048-bit RSA)
        // Plus 8 bytes for length fields = ~304 bytes = ~608 hex characters
        assertTrue(response.getEncryptedDataHex().length() > 500);
    }

    @Test
    void testMultipleEncryptions() throws Exception {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        KeyPair keyPair = keyPairGen.generateKeyPair();
        String publicKeyHex = HexUtil.bytesToHex(keyPair.getPublic().getEncoded());

        String[] cardNumbers = {
                "4532123456789012",
                "5425233430109903",
                "374245455400126"
        };

        for (String cardNumber : cardNumbers) {
            EncryptionRequest request = new EncryptionRequest(publicKeyHex, cardNumber);
            EncryptionResponse response = cryptoService.encryptCardNumber(request);

            assertTrue(response.isSuccess(), "Encryption should succeed for card: " + cardNumber);
            assertNotNull(response.getEncryptedDataHex());
        }
    }

    @Test
    void testDifferentRSAKeySizes() throws Exception {
        String cardNumber = "4532123456789012";

        int[] keySizes = { 2048, 3072, 4096 };

        for (int keySize : keySizes) {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(keySize);
            KeyPair keyPair = keyPairGen.generateKeyPair();
            String publicKeyHex = HexUtil.bytesToHex(keyPair.getPublic().getEncoded());

            EncryptionRequest request = new EncryptionRequest(publicKeyHex, cardNumber);
            EncryptionResponse response = cryptoService.encryptCardNumber(request);

            assertTrue(response.isSuccess(), "Encryption should succeed with " + keySize + "-bit RSA key");
            assertNotNull(response.getEncryptedDataHex());
        }
    }

    @Test
    void testErrorHandling() {
        // Test with invalid public key
        EncryptionRequest invalidRequest = new EncryptionRequest("INVALID_HEX", "4532123456789012");
        EncryptionResponse response = cryptoService.encryptCardNumber(invalidRequest);

        assertFalse(response.isSuccess());
        assertNotNull(response.getErrorMessage());
        assertNull(response.getEncryptedDataHex());
    }
}
