# RPGLE Integration Guide for Common Crypto Service

This guide provides complete instructions for integrating the Common Crypto Service with RPGLE programs on iSeries using JNI (Java Native Interface).

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [JAR Deployment](#jar-deployment)
4. [Environment Setup](#environment-setup)
5. [RPGLE Code Examples](#rpgle-code-examples)
6. [Error Handling](#error-handling)
7. [Performance Considerations](#performance-considerations)
8. [Troubleshooting](#troubleshooting)

---

## Overview

The Common Crypto Service provides AES-GCM encryption for sensitive card data with RSA-OAEP key encryption. The JNI wrapper (`JniCryptoWrapper`) exposes a simple static method that RPGLE programs can call directly.

**Key Benefits:**
- ✅ Simple static method call - no object lifecycle management
- ✅ Clear error codes (0-4) instead of exceptions
- ✅ Thread-safe implementation
- ✅ No memory leak concerns
- ✅ Minimal RPGLE code required

**Architecture:**
```
RPGLE Program → JNI → JniCryptoWrapper.encryptCardNumber() → CryptoService → Encrypted Data
```

---

## Prerequisites

### iSeries Requirements
- IBM i OS version 7.2 or higher
- Java Runtime Environment (JRE) 17 or higher installed
- Authority to upload files to IFS
- Authority to modify job environment variables

### Java JAR File
Build the JAR file using Maven:
```bash
mvn clean package
```

This creates: `target/common-crypto-service-1.0-SNAPSHOT-with-dependencies.jar`

---

## JAR Deployment

### Step 1: Upload JAR to IFS

Upload the JAR file to a directory on the IFS, for example:
```
/crypto/lib/common-crypto-service-1.0-SNAPSHOT-with-dependencies.jar
```

**Using FTP:**
```
ftp your-iseries-host
> cd /crypto/lib
> bin
> put common-crypto-service-1.0-SNAPSHOT-with-dependencies.jar
> quit
```

**Using CPYTOSTMF (from PC to IFS):**
```
CPYTOSTMF FROMMBR('/QSYS.LIB/YOURLIB.LIB/QCLSRC.FILE/JARFILE.MBR') +
          TOSTMF('/crypto/lib/common-crypto-service-1.0-SNAPSHOT-with-dependencies.jar') +
          STMFOPT(*REPLACE) CVTDTA(*NONE)
```

### Step 2: Set Permissions

Ensure the JAR file has appropriate read permissions:
```bash
chmod 755 /crypto/lib/common-crypto-service-1.0-SNAPSHOT-with-dependencies.jar
```

---

## Environment Setup

### Option 1: Set CLASSPATH in Job (Recommended)

Add the JAR to your job's CLASSPATH using `ADDENVVAR`:

```
ADDENVVAR ENVVAR(CLASSPATH) +
          VALUE('/crypto/lib/common-crypto-service-1.0-SNAPSHOT-with-dependencies.jar') +
          LEVEL(*JOB)
```

**For multiple JARs:**
```
ADDENVVAR ENVVAR(CLASSPATH) +
          VALUE('/crypto/lib/common-crypto-service-1.0-SNAPSHOT-with-dependencies.jar:/other/lib/another.jar') +
          LEVEL(*JOB)
```

**Note:** Use colon (`:`) as the separator for multiple JAR files.

### Option 2: Set CLASSPATH in Job Description

For permanent setup, add to your job description:

```
CHGJOBD JOBD(YOURLIB/YOURJOBD) +
        ENVVAR(('CLASSPATH' '/crypto/lib/common-crypto-service-1.0-SNAPSHOT-with-dependencies.jar'))
```

### Option 3: Set in CL Program

Add this to your CL program before calling the RPGLE program:

```cl
PGM

  /* Set Java CLASSPATH */
  ADDENVVAR ENVVAR(CLASSPATH) +
            VALUE('/crypto/lib/common-crypto-service-1.0-SNAPSHOT-with-dependencies.jar') +
            LEVEL(*JOB)

  /* Call your RPGLE program */
  CALL PGM(YOURLIB/YOURPGM)

ENDPGM
```

### Verify CLASSPATH

To verify the CLASSPATH is set correctly:
```
WRKENVVAR LEVEL(*JOB)
```

Look for `CLASSPATH` in the list.

---

## RPGLE Code Examples

### Example 1: Basic Encryption

```rpgle
**FREE

// Prototypes for Java classes
DCL-PR JniCryptoWrapper_encryptCardNumber OBJECT(*JAVA:'com.crypto.jni.JniEncryptionResult')
                                          EXTPROC(*JAVA:
                                                  'com.crypto.jni.JniCryptoWrapper':
                                                  'encryptCardNumber')
                                          STATIC;
  cardNumber VARCHAR(100) CONST;
  publicKeyHex VARCHAR(10000) CONST;
END-PR;

DCL-PR JniEncryptionResult_getErrorCode INT(10) EXTPROC(*JAVA:
                                                        'com.crypto.jni.JniEncryptionResult':
                                                        'getErrorCode');
  result OBJECT(*JAVA:'com.crypto.jni.JniEncryptionResult');
END-PR;

DCL-PR JniEncryptionResult_getEncryptedDataHex VARCHAR(32767) EXTPROC(*JAVA:
                                                                      'com.crypto.jni.JniEncryptionResult':
                                                                      'getEncryptedDataHex');
  result OBJECT(*JAVA:'com.crypto.jni.JniEncryptionResult');
END-PR;

DCL-PR JniEncryptionResult_getErrorMessage VARCHAR(1000) EXTPROC(*JAVA:
                                                                  'com.crypto.jni.JniEncryptionResult':
                                                                  'getErrorMessage');
  result OBJECT(*JAVA:'com.crypto.jni.JniEncryptionResult');
END-PR;

// Main program variables
DCL-S result OBJECT(*JAVA:'com.crypto.jni.JniEncryptionResult');
DCL-S cardNumber VARCHAR(100);
DCL-S publicKeyHex VARCHAR(10000);
DCL-S errorCode INT(10);
DCL-S encryptedData VARCHAR(32767);
DCL-S errorMessage VARCHAR(1000);

// Your card number (in production, read from database/input)
cardNumber = '4532123456789012';

// Your RSA public key in hex format (in production, read from secure storage)
publicKeyHex = '308201...'; // Full hex string of RSA public key

// Call the encryption method
result = JniCryptoWrapper_encryptCardNumber(cardNumber: publicKeyHex);

// Check error code
errorCode = JniEncryptionResult_getErrorCode(result);

IF errorCode = 0;
  // Success - get encrypted data
  encryptedData = JniEncryptionResult_getEncryptedDataHex(result);
  
  // Process encrypted data (e.g., store in database, send to API)
  // ...
  
  DSPLY ('Encryption successful');
ELSE;
  // Error - get error message
  errorMessage = JniEncryptionResult_getErrorMessage(result);
  
  // Log or handle error
  DSPLY ('Encryption failed: ' + errorMessage);
ENDIF;

*INLR = *ON;
```

### Example 2: Batch Processing with Error Handling

```rpgle
**FREE

// Prototypes (same as Example 1)
DCL-PR JniCryptoWrapper_encryptCardNumber OBJECT(*JAVA:'com.crypto.jni.JniEncryptionResult')
                                          EXTPROC(*JAVA:
                                                  'com.crypto.jni.JniCryptoWrapper':
                                                  'encryptCardNumber')
                                          STATIC;
  cardNumber VARCHAR(100) CONST;
  publicKeyHex VARCHAR(10000) CONST;
END-PR;

DCL-PR JniEncryptionResult_getErrorCode INT(10) EXTPROC(*JAVA:
                                                        'com.crypto.jni.JniEncryptionResult':
                                                        'getErrorCode');
  result OBJECT(*JAVA:'com.crypto.jni.JniEncryptionResult');
END-PR;

DCL-PR JniEncryptionResult_getEncryptedDataHex VARCHAR(32767) EXTPROC(*JAVA:
                                                                      'com.crypto.jni.JniEncryptionResult':
                                                                      'getEncryptedDataHex');
  result OBJECT(*JAVA:'com.crypto.jni.JniEncryptionResult');
END-PR;

DCL-PR JniEncryptionResult_getErrorMessage VARCHAR(1000) EXTPROC(*JAVA:
                                                                  'com.crypto.jni.JniEncryptionResult':
                                                                  'getErrorMessage');
  result OBJECT(*JAVA:'com.crypto.jni.JniEncryptionResult');
END-PR;

// File definition for card data
DCL-F CARDFILE DISK USAGE(*INPUT) KEYED;

// Variables
DCL-S result OBJECT(*JAVA:'com.crypto.jni.JniEncryptionResult');
DCL-S publicKeyHex VARCHAR(10000);
DCL-S errorCode INT(10);
DCL-S encryptedData VARCHAR(32767);
DCL-S errorMessage VARCHAR(1000);
DCL-S successCount INT(10) INZ(0);
DCL-S failureCount INT(10) INZ(0);

// Load RSA public key once (from configuration file or database)
publicKeyHex = loadPublicKey();

// Process all cards
DOW NOT %EOF(CARDFILE);
  READ CARDFILE;
  
  IF NOT %EOF(CARDFILE);
    // Encrypt card number
    result = JniCryptoWrapper_encryptCardNumber(CARDNBR: publicKeyHex);
    errorCode = JniEncryptionResult_getErrorCode(result);
    
    IF errorCode = 0;
      // Success
      encryptedData = JniEncryptionResult_getEncryptedDataHex(result);
      
      // Update database with encrypted data
      EXEC SQL UPDATE CARDFILE 
               SET ENCRYPTED_DATA = :encryptedData,
                   ENCRYPT_STATUS = 'SUCCESS'
               WHERE CARD_ID = :CARDID;
      
      successCount += 1;
    ELSE;
      // Error
      errorMessage = JniEncryptionResult_getErrorMessage(result);
      
      // Log error
      EXEC SQL INSERT INTO ERROR_LOG 
               (CARD_ID, ERROR_CODE, ERROR_MESSAGE, TIMESTAMP)
               VALUES (:CARDID, :errorCode, :errorMessage, CURRENT_TIMESTAMP);
      
      failureCount += 1;
    ENDIF;
  ENDIF;
ENDDO;

// Display summary
DSPLY ('Processed: ' + %CHAR(successCount + failureCount));
DSPLY ('Success: ' + %CHAR(successCount));
DSPLY ('Failures: ' + %CHAR(failureCount));

*INLR = *ON;

// Subprocedure to load public key
DCL-PROC loadPublicKey;
  DCL-PI *N VARCHAR(10000);
  END-PI;
  
  DCL-S key VARCHAR(10000);
  
  // Read from IFS file
  EXEC SQL SELECT PUBLIC_KEY INTO :key
           FROM CONFIG_TABLE
           WHERE CONFIG_NAME = 'RSA_PUBLIC_KEY';
  
  RETURN key;
END-PROC;
```

### Example 3: Test Connection

Before implementing full encryption, test your JNI setup:

```rpgle
**FREE

// Prototype for test method
DCL-PR JniCryptoWrapper_testConnection OBJECT(*JAVA:'com.crypto.jni.JniEncryptionResult')
                                       EXTPROC(*JAVA:
                                               'com.crypto.jni.JniCryptoWrapper':
                                               'testConnection')
                                       STATIC;
  testInput VARCHAR(100) CONST;
END-PR;

DCL-PR JniEncryptionResult_getErrorCode INT(10) EXTPROC(*JAVA:
                                                        'com.crypto.jni.JniEncryptionResult':
                                                        'getErrorCode');
  result OBJECT(*JAVA:'com.crypto.jni.JniEncryptionResult');
END-PR;

DCL-PR JniEncryptionResult_getEncryptedDataHex VARCHAR(32767) EXTPROC(*JAVA:
                                                                      'com.crypto.jni.JniEncryptionResult':
                                                                      'getEncryptedDataHex');
  result OBJECT(*JAVA:'com.crypto.jni.JniEncryptionResult');
END-PR;

// Variables
DCL-S result OBJECT(*JAVA:'com.crypto.jni.JniEncryptionResult');
DCL-S errorCode INT(10);
DCL-S resultData VARCHAR(32767);

// Test connection
result = JniCryptoWrapper_testConnection('Hello from iSeries');
errorCode = JniEncryptionResult_getErrorCode(result);

IF errorCode = 0;
  resultData = JniEncryptionResult_getEncryptedDataHex(result);
  DSPLY ('Test successful: ' + resultData);
ELSE;
  DSPLY ('Test failed - check CLASSPATH and JAR deployment');
ENDIF;

*INLR = *ON;
```

---

## Error Handling

### Error Codes

| Code | Constant | Description | Recommended Action |
|------|----------|-------------|-------------------|
| 0 | SUCCESS | Operation completed successfully | Process encrypted data |
| 1 | INVALID_INPUT | Card number is null or empty | Validate input before calling |
| 2 | INVALID_PUBLIC_KEY | RSA public key is null, empty, or malformed | Check public key format and source |
| 3 | ENCRYPTION_FAILED | Encryption operation failed | Log error, retry or alert |
| 4 | UNEXPECTED_ERROR | Unexpected error occurred | Log error, contact support |

### Error Handling Pattern

```rpgle
result = JniCryptoWrapper_encryptCardNumber(cardNumber: publicKeyHex);
errorCode = JniEncryptionResult_getErrorCode(result);

SELECT;
  WHEN errorCode = 0;
    // SUCCESS - process encrypted data
    encryptedData = JniEncryptionResult_getEncryptedDataHex(result);
    // ... continue processing
    
  WHEN errorCode = 1;
    // INVALID_INPUT - validation error
    errorMessage = JniEncryptionResult_getErrorMessage(result);
    // Log validation error, fix input
    
  WHEN errorCode = 2;
    // INVALID_PUBLIC_KEY - configuration error
    errorMessage = JniEncryptionResult_getErrorMessage(result);
    // Check public key configuration
    
  WHEN errorCode = 3;
    // ENCRYPTION_FAILED - crypto error
    errorMessage = JniEncryptionResult_getErrorMessage(result);
    // Log error, may retry
    
  WHEN errorCode = 4;
    // UNEXPECTED_ERROR - system error
    errorMessage = JniEncryptionResult_getErrorMessage(result);
    // Log error, alert support
    
  OTHER;
    // Unknown error code
    DSPLY ('Unknown error code: ' + %CHAR(errorCode));
ENDSL;
```

---

## Performance Considerations

### Thread Safety
- ✅ The `JniCryptoWrapper` is **fully thread-safe**
- ✅ Each call creates a new `CryptoService` instance
- ✅ No shared state between calls
- ✅ Safe for concurrent batch processing

### Best Practices

1. **Reuse Public Key**
   ```rpgle
   // Load once
   publicKeyHex = loadPublicKey();
   
   // Reuse for all encryptions
   DOW NOT %EOF(FILE);
     result = JniCryptoWrapper_encryptCardNumber(cardNumber: publicKeyHex);
     // ...
   ENDDO;
   ```

2. **Batch Processing**
   - Process multiple cards in a single job
   - Commit database updates in batches
   - Log errors for later review

3. **Memory Management**
   - JNI handles Java object cleanup automatically
   - No need to manually free Java objects
   - RPGLE variables are cleaned up normally

4. **Error Logging**
   - Always log encryption failures
   - Include card ID (not card number) in logs
   - Track error codes for monitoring

### Performance Expectations

- **Single encryption**: ~50-100ms (depends on RSA key size and system load)
- **Batch processing**: Can process 10-20 cards per second
- **Memory usage**: Minimal - each call is stateless

---

## Troubleshooting

### Issue: "Java class not found" Error

**Cause:** CLASSPATH not set correctly or JAR not uploaded

**Solution:**
1. Verify JAR exists on IFS:
   ```
   WRKLNK OBJ('/crypto/lib/*')
   ```

2. Check CLASSPATH:
   ```
   WRKENVVAR LEVEL(*JOB)
   ```

3. Verify CLASSPATH includes full path to JAR

### Issue: "Method not found" Error

**Cause:** Incorrect method signature in RPGLE prototype

**Solution:**
- Verify prototype matches Java method exactly
- Check class name: `com.crypto.jni.JniCryptoWrapper`
- Check method name: `encryptCardNumber`
- Ensure `STATIC` keyword is present

### Issue: Error Code 2 (INVALID_PUBLIC_KEY)

**Cause:** Public key is not in correct format

**Solution:**
- Public key must be in **hex format** (X.509 encoded)
- Generate using Java:
  ```java
  String publicKeyHex = HexFormat.of().withUpperCase()
                                .formatHex(publicKey.getEncoded());
  ```
- Verify no extra spaces or line breaks in hex string

### Issue: Performance is Slow

**Cause:** Creating new key pairs or loading public key repeatedly

**Solution:**
- Load public key **once** at program start
- Reuse the same public key for all encryptions in a batch
- Consider using smaller RSA key size (2048 bits recommended)

### Issue: Memory Leaks

**Cause:** Typically not an issue with this implementation

**Solution:**
- The static method design prevents memory leaks
- Each call is stateless and self-contained
- JVM garbage collection handles cleanup

---

## Additional Resources

### Java Documentation
- See `README.md` for detailed crypto service documentation
- Review `CryptoServiceImpl.java` for encryption algorithm details

### Support
For issues or questions:
1. Check error logs in `/crypto/logs/`
2. Review Java logs (if logging is enabled)
3. Contact your Java development team

### Security Notes
- **Never log card numbers** - only log card IDs or masked values
- **Protect public keys** - store in secure configuration
- **Use HTTPS** when transmitting encrypted data
- **Rotate keys** regularly according to your security policy

---

## Quick Reference

### Minimum RPGLE Code

```rpgle
**FREE

// Prototypes
DCL-PR JniCryptoWrapper_encryptCardNumber OBJECT(*JAVA:'com.crypto.jni.JniEncryptionResult')
                                          EXTPROC(*JAVA:'com.crypto.jni.JniCryptoWrapper':'encryptCardNumber') STATIC;
  cardNumber VARCHAR(100) CONST;
  publicKeyHex VARCHAR(10000) CONST;
END-PR;

DCL-PR JniEncryptionResult_getErrorCode INT(10) 
       EXTPROC(*JAVA:'com.crypto.jni.JniEncryptionResult':'getErrorCode');
  result OBJECT(*JAVA:'com.crypto.jni.JniEncryptionResult');
END-PR;

DCL-PR JniEncryptionResult_getEncryptedDataHex VARCHAR(32767) 
       EXTPROC(*JAVA:'com.crypto.jni.JniEncryptionResult':'getEncryptedDataHex');
  result OBJECT(*JAVA:'com.crypto.jni.JniEncryptionResult');
END-PR;

// Variables
DCL-S result OBJECT(*JAVA:'com.crypto.jni.JniEncryptionResult');
DCL-S errorCode INT(10);
DCL-S encryptedData VARCHAR(32767);

// Encrypt
result = JniCryptoWrapper_encryptCardNumber('4532123456789012': 'YOUR_PUBLIC_KEY_HEX');
errorCode = JniEncryptionResult_getErrorCode(result);

IF errorCode = 0;
  encryptedData = JniEncryptionResult_getEncryptedDataHex(result);
ENDIF;

*INLR = *ON;
```

### ADDENVVAR Command

```
ADDENVVAR ENVVAR(CLASSPATH) +
          VALUE('/crypto/lib/common-crypto-service-1.0-SNAPSHOT-with-dependencies.jar') +
          LEVEL(*JOB)
```
