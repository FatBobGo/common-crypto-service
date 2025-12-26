package com.crypto.service;

import com.crypto.exception.CryptoException;
import com.crypto.model.EncryptionRequest;
import com.crypto.model.EncryptionResponse;
import com.crypto.service.impl.CryptoServiceImpl;
import com.crypto.util.HexUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CryptoService.
 */
class CryptoServiceTest {

    private CryptoService cryptoService;
    private KeyPair rsaKeyPair;
    private String publicKeyHex;

    @BeforeEach
    void setUp() throws Exception {
        cryptoService = new CryptoServiceImpl();

        // Generate RSA key pair for testing
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        rsaKeyPair = keyPairGen.generateKeyPair();
        publicKeyHex = HexUtil.bytesToHex(rsaKeyPair.getPublic().getEncoded());
    }

    @Test
    void testGenerateAESKey() throws CryptoException {
        byte[] aesKey = cryptoService.generateAESKey();

        assertNotNull(aesKey);
        assertEquals(32, aesKey.length); // 256 bits = 32 bytes
    }

    @Test
    void testGenerateAESKey_Uniqueness() throws CryptoException {
        byte[] key1 = cryptoService.generateAESKey();
        byte[] key2 = cryptoService.generateAESKey();

        assertFalse(MessageDigest.isEqual(key1, key2), "Generated keys should be unique");
    }

    @Test
    void testEncryptCardNumber_Success() {
        String cardNumber = "4532123456789012";
        EncryptionRequest request = new EncryptionRequest(publicKeyHex, cardNumber);

        EncryptionResponse response = cryptoService.encryptCardNumber(request);

        assertTrue(response.isSuccess());
        assertNotNull(response.getEncryptedDataHex());
        assertNull(response.getErrorMessage());
        assertTrue(response.getEncryptedDataHex().length() > 0);
    }

    @Test
    void testEncryptCardNumber_DifferentOutputs() {
        String cardNumber = "4532123456789012";
        EncryptionRequest request1 = new EncryptionRequest(publicKeyHex, cardNumber);
        EncryptionRequest request2 = new EncryptionRequest(publicKeyHex, cardNumber);

        EncryptionResponse response1 = cryptoService.encryptCardNumber(request1);
        EncryptionResponse response2 = cryptoService.encryptCardNumber(request2);

        assertTrue(response1.isSuccess());
        assertTrue(response2.isSuccess());
        assertNotEquals(response1.getEncryptedDataHex(), response2.getEncryptedDataHex(),
                "Each encryption should produce different output due to random IV and AES key");
    }

    @Test
    void testEncryptCardNumber_NullRequest() {
        EncryptionResponse response = cryptoService.encryptCardNumber(null);

        assertFalse(response.isSuccess());
        assertNotNull(response.getErrorMessage());
        assertTrue(response.getErrorMessage().contains("null"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "  ", "\t", "\n" })
    void testEncryptCardNumber_InvalidCardNumber(String invalidCardNumber) {
        EncryptionRequest request = new EncryptionRequest(publicKeyHex, invalidCardNumber);

        EncryptionResponse response = cryptoService.encryptCardNumber(request);

        assertFalse(response.isSuccess());
        assertNotNull(response.getErrorMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "  ", "\t" })
    void testEncryptCardNumber_InvalidPublicKey(String invalidKey) {
        EncryptionRequest request = new EncryptionRequest(invalidKey, "4532123456789012");

        EncryptionResponse response = cryptoService.encryptCardNumber(request);

        assertFalse(response.isSuccess());
        assertNotNull(response.getErrorMessage());
    }

    @Test
    void testEncryptCardNumber_MalformedPublicKey() {
        EncryptionRequest request = new EncryptionRequest("INVALIDHEX", "4532123456789012");

        EncryptionResponse response = cryptoService.encryptCardNumber(request);

        assertFalse(response.isSuccess());
        assertNotNull(response.getErrorMessage());
    }

    @Test
    void testEncryptCardNumber_CompleteFlow() throws Exception {
        String cardNumber = "4532123456789012";
        EncryptionRequest request = new EncryptionRequest(publicKeyHex, cardNumber);

        EncryptionResponse response = cryptoService.encryptCardNumber(request);

        assertTrue(response.isSuccess());

        // Decrypt and verify the complete flow
        byte[] combinedData = HexUtil.hexToBytes(response.getEncryptedDataHex());
        ByteBuffer buffer = ByteBuffer.wrap(combinedData);

        /**
            [4 bytes: IV Length]
            [12 bytes: IV]
            [4 bytes: Encrypted Card Length]
            [32 bytes: Encrypted Card Data (includes GCM tag automatically appended by cipher)]
            [256 bytes: Encrypted AES Key (for 2048-bit RSA)]
         */
        
        // Extract IV
        int ivLength = buffer.getInt();
        byte[] iv = new byte[ivLength];
        buffer.get(iv);
        assertEquals(12, ivLength); // GCM IV should be 12 bytes

        // Extract encrypted card
        int encryptedCardLength = buffer.getInt();
        byte[] encryptedCard = new byte[encryptedCardLength];
        buffer.get(encryptedCard);

        // Extract encrypted AES key
        byte[] encryptedAESKey = new byte[buffer.remaining()];
        buffer.get(encryptedAESKey);

        // Decrypt AES key using RSA private key
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", "BC");
        rsaCipher.init(Cipher.DECRYPT_MODE, rsaKeyPair.getPrivate());
        byte[] aesKey = rsaCipher.doFinal(encryptedAESKey);

        assertEquals(32, aesKey.length); // AES-256 key should be 32 bytes

        // Decrypt card number using AES key
        SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        aesCipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
        byte[] decryptedCardBytes = aesCipher.doFinal(encryptedCard);
        String decryptedCard = new String(decryptedCardBytes, "UTF-8");

        assertEquals(cardNumber, decryptedCard);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "4532123456789012",
            "5425233430109903",
            "374245455400126",
            "6011111111111117"
    })
    void testEncryptCardNumber_VariousCardNumbers(String cardNumber) {
        EncryptionRequest request = new EncryptionRequest(publicKeyHex, cardNumber);

        EncryptionResponse response = cryptoService.encryptCardNumber(request);

        assertTrue(response.isSuccess());
        assertNotNull(response.getEncryptedDataHex());
    }
}
