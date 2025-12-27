# RPGLE Integration

```
I will package the java app into a `jar` app and upload to iSeries IFS folder so that i can use the RPGLE to call the Java service to perform the encryption, as it is super hard to implement such in RPGLE language. 

The iSeries RPGLE will use the JNI to call the java program. To cater for the memory leak issue, I plan to create a static method in Java app to make the RPGLE program consume the Java program more easy. 

I want to make the coding in RPGLE as simple as possbile. So the design is:

I pass the clear card number to the static function then the function will return the encrypted data and error code which is to indicate success or fail.

Behind the sceene, the java program will follow the existing logic to encrypt the card number / sensitive info.
```