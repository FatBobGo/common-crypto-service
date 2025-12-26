# Common Crypto Service

A Java-based cryptographic service that encrypts debit card numbers using AES-256-GCM encryption, with the AES key encrypted using RSA-OAEP with MGF1 padding.

## Overview

This service provides secure encryption of sensitive card data for API communication. The encryption flow ensures that:
- Each encryption uses a unique, randomly generated AES key
- Card numbers are encrypted using authenticated encryption (AES-GCM)
- The AES key is securely encrypted using the consumer's RSA public key
- All data is transmitted in hexadecimal format

## Encryption Flow

```
1. API Consumer → Backend: Send RSA public key (hex format)
2. Backend: Generate random AES-256 key
3. Backend: Encrypt card number with AES-GCM
4. Backend: Encrypt AES key with RSA public key (OAEP + MGF1)
5. Backend → Consumer: Return combined encrypted data (hex format)
```

### Data Format

The encrypted output is a hexadecimal string containing:
```
[IV_LENGTH (4 bytes)][IV (12 bytes)][ENCRYPTED_CARD_LENGTH (4 bytes)][ENCRYPTED_CARD][ENCRYPTED_AES_KEY]
```

## Technical Specifications

### Cryptographic Algorithms

- **AES Encryption**: AES/GCM/NoPadding with 256-bit keys
- **RSA Encryption**: RSA/ECB/OAEPWithSHA-256AndMGF1Padding
- **IV Size**: 12 bytes (96 bits) for GCM mode
- **GCM Tag**: 128 bits for authentication
- **Provider**: Bouncy Castle

### Dependencies

- **JDK**: 17
- **Build Tool**: Maven
- **Logging**: SLF4J 2.0.16 + Logback 1.5.12
- **Crypto**: Bouncy Castle 1.79
- **Testing**: JUnit 5.11.3, WireMock 3.10.0

## Installation

### Prerequisites

- JDK 17 or higher
- Maven 3.6+

### Build

```bash
# Clone or navigate to the project directory
cd common-crypto-service

# Compile the project
mvn clean compile

# Run tests
mvn test

# Build JAR
mvn clean package
```

## Usage

### Basic Example

```java
import com.crypto.model.EncryptionRequest;
import com.crypto.model.EncryptionResponse;
import com.crypto.service.CryptoService;
import com.crypto.service.impl.CryptoServiceImpl;
import com.crypto.util.HexUtil;

// Initialize the service
CryptoService cryptoService = new CryptoServiceImpl();

// Prepare request (public key from consumer in hex format)
String rsaPublicKeyHex = "3082010A02820101..."; // Consumer's RSA public key
String cardNumber = "4532123456789012";

EncryptionRequest request = new EncryptionRequest(rsaPublicKeyHex, cardNumber);

// Encrypt
EncryptionResponse response = cryptoService.encryptCardNumber(request);

if (response.isSuccess()) {
    String encryptedDataHex = response.getEncryptedDataHex();
    System.out.println("Encrypted data: " + encryptedDataHex);
} else {
    System.err.println("Encryption failed: " + response.getErrorMessage());
}
```

### Running the Demo

```bash
# Compile and run the demo
mvn compile exec:java -Dexec.mainClass="com.crypto.CryptoDemo"
```

## Decryption (Consumer Side)

The API consumer can decrypt the data using their RSA private key:

```java
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.PrivateKey;

// Parse the encrypted data
byte[] combinedData = HexUtil.hexToBytes(encryptedDataHex);
ByteBuffer buffer = ByteBuffer.wrap(combinedData);

// Extract IV
int ivLength = buffer.getInt();
byte[] iv = new byte[ivLength];
buffer.get(iv);

// Extract encrypted card
int encryptedCardLength = buffer.getInt();
byte[] encryptedCard = new byte[encryptedCardLength];
buffer.get(encryptedCard);

// Extract encrypted AES key
byte[] encryptedAESKey = new byte[buffer.remaining()];
buffer.get(encryptedAESKey);

// Decrypt AES key using RSA private key
Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", "BC");
rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
byte[] aesKey = rsaCipher.doFinal(encryptedAESKey);

// Decrypt card number using AES key
SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
aesCipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
byte[] decryptedCardBytes = aesCipher.doFinal(encryptedCard);
String cardNumber = new String(decryptedCardBytes, "UTF-8");
```

## Testing

### Run All Tests

```bash
mvn test
```

### Test Coverage

The project includes:
- **Unit Tests**: `HexUtilTest`, `CryptoServiceTest`
- **Integration Tests**: `IntegrationTest`

Tests cover:
- AES key generation and uniqueness
- AES-GCM encryption/decryption
- RSA-OAEP encryption with MGF1
- Hex encoding/decoding
- Complete end-to-end flow
- Error handling and validation
- Various card numbers and RSA key sizes

## Logging

Logs are written to:
- **Console**: All log levels
- **File**: `logs/crypto-service.log` with daily rotation (max 10MB per file, 30 days retention)

Log levels:
- `com.crypto` package: DEBUG
- Root: INFO

## Security Considerations

1. **Key Management**: RSA private keys must be securely stored by the API consumer
2. **Random Generation**: Each encryption generates a new random AES key and IV
3. **Authenticated Encryption**: GCM mode provides both confidentiality and authenticity
4. **No Key Reuse**: AES keys are never reused across encryptions
5. **Secure Padding**: RSA uses OAEP with SHA-256 and MGF1 for robust padding

## Project Structure

```
common-crypto-service/
├── src/
│   ├── main/
│   │   ├── java/com/crypto/
│   │   │   ├── constants/
│   │   │   │   └── CryptoConstants.java
│   │   │   ├── exception/
│   │   │   │   └── CryptoException.java
│   │   │   ├── model/
│   │   │   │   ├── EncryptionRequest.java
│   │   │   │   └── EncryptionResponse.java
│   │   │   ├── service/
│   │   │   │   ├── CryptoService.java
│   │   │   │   └── impl/
│   │   │   │       └── CryptoServiceImpl.java
│   │   │   ├── util/
│   │   │   │   └── HexUtil.java
│   │   │   └── CryptoDemo.java
│   │   └── resources/
│   │       └── logback.xml
│   └── test/
│       └── java/com/crypto/
│           ├── integration/
│           │   └── IntegrationTest.java
│           ├── service/
│           │   └── CryptoServiceTest.java
│           └── util/
│               └── HexUtilTest.java
└── pom.xml
```

## License

This project is provided as-is for demonstration purposes.

## Support

For issues or questions, please refer to the project documentation or contact the development team.
