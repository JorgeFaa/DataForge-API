package com.dataforge.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class EncryptionUtil {

    private static SecretKeySpec secretKey;
    private static byte[] key;
    private static final String ALGORITHM = "AES";

    // ===================================================================
    // !!! IMPORTANT SECURITY NOTICE !!!
    // ===================================================================
    // This is the secret key used to encrypt the passwords of the databases you create.
    // In a production environment, this key should NOT be hardcoded.
    // It should be loaded securely from environment variables or a key vault.
    //
    // !!! REPLACE THIS WITH YOUR OWN SECRET KEY !!!
    private static final String SECRET_KEY_STRING = "ThisIsASecretKeyForDataforgeAPI"; 

    public EncryptionUtil() {
        prepareSecretKey(SECRET_KEY_STRING);
    }

    private static void prepareSecretKey(String myKey) {
        MessageDigest sha = null;
        try {
            key = myKey.getBytes(StandardCharsets.UTF_8);
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); // Use only first 128 bit (16 bytes) for AES
            secretKey = new SecretKeySpec(key, ALGORITHM);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String encrypt(String strToEncrypt) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            System.out.println("Error while encrypting: " + e.toString());
        }
        return null;
    }

    public String decrypt(String strToDecrypt) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        } catch (Exception e) {
            System.out.println("Error while decrypting: " + e.toString());
        }
        return null;
    }
}
