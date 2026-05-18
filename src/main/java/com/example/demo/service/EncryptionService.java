package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Szyfrowanie AES-256-GCM.
 * Format zaszyfrowanego tekstu: Base64(IV[12 bajtów] + CipherText + AuthTag[16 bajtów])
 */
@Service
public class EncryptionService {

    private static final String ALGORITHM    = "AES/GCM/NoPadding";
    private static final int    IV_LENGTH    = 12;   // bajtów (96 bitów — standard GCM)
    private static final int    TAG_LENGTH   = 128;  // bitów

    private final SecretKey secretKey;

    public EncryptionService(@Value("${app.encryption.key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key.trim());
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                "Klucz szyfrowania musi mieć dokładnie 32 bajty (256 bitów). " +
                "Aktualny rozmiar: " + keyBytes.length + " bajtów."
            );
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);  // losowe IV dla każdej wiadomości

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));

            byte[] cipherBytes = cipher.doFinal(plainText.getBytes("UTF-8"));

            // Złącz IV + ciphertext+tag
            byte[] combined = new byte[IV_LENGTH + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(cipherBytes, 0, combined, IV_LENGTH, cipherBytes.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Błąd szyfrowania wiadomości", e);
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null) return null;
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            byte[] iv          = new byte[IV_LENGTH];
            byte[] cipherBytes = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, cipherBytes, 0, cipherBytes.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Błąd deszyfrowania wiadomości", e);
        }
    }
}
