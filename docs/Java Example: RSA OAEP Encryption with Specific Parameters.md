# RSA OAEP Encryption with Specific Parameters

In Java, the OAEPParameterSpec class is used to explicitly define the parameters for Optimal Asymmetric Encryption Padding (OAEP). It is most often used when you need to ensure interoperability between different systems (e.g., Java and C# or JavaScript) that might have different default settings for the internal hash functions. 

## Java Example: RSA OAEP Encryption with Specific Parameters

The following example demonstrates how to use OAEPParameterSpec to specify that both the main message digest and the Mask Generation Function 1 (MGF1) digest should use "SHA-256". 

[bouncy-castle-vs-java-default-rsa-with-oaep](https://stackoverflow.com/questions/50298687/bouncy-castle-vs-java-default-rsa-with-oaep)


```java
import javax.crypto.Cipher;
import javax.crypto.spec.MGF1ParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Base64;

public class OAEPExample {

    public static void main(String[] args) throws Exception {
        // 1. Generate an RSA key pair (for demonstration purposes)
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();

        // 2. Define the message to encrypt
        String originalMessage = "This is a secret message.";
        byte[] messageBytes = originalMessage.getBytes("UTF-8");

        // 3. Define the OAEP parameters with SHA-256 for both digests
        // The constructor takes:
        // - mdName (message digest algorithm name) -> "SHA-256"
        // - mgfName (mask generation function algorithm name) -> "MGF1"
        // - mgfSpec (parameters for the MGF) -> MGF1ParameterSpec.SHA256
        // - pSrc (source of the encoding input P) -> PSource.PSpecified.DEFAULT
        OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT
        );

        // 4. Initialize the Cipher with the specific parameters
        // The transformation "RSA/ECB/OAEPPadding" is general, and the
        // parameters are passed in the init method.
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams);

        // 5. Encrypt the data
        byte[] encryptedData = cipher.doFinal(messageBytes);
        String encryptedString = Base64.getEncoder().encodeToString(encryptedData);

        System.out.println("Original Message: " + originalMessage);
        System.out.println("Encrypted Data (Base64): " + encryptedString);
        
        // Note: The corresponding decryption would use the private key 
        // and the *same* OAEPParameterSpec instance to ensure compatibility.
    }
}

```