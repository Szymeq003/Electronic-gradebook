package com.example.demo.config;

import com.example.demo.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA Converter — automatycznie szyfruje/deszyfruje kolumny oznaczone @Convert.
 * autoApply = false — stosujemy tylko tam gdzie chcemy (@Convert na polu w encji).
 */
@Converter
@Component
public class MessageEncryptionConverter implements AttributeConverter<String, String> {

    // static, bo JPA tworzy konwerter poza kontekstem Springa
    private static EncryptionService encryptionService;

    @Autowired
    public void setEncryptionService(EncryptionService svc) {
        MessageEncryptionConverter.encryptionService = svc;
    }

    @Override
    public String convertToDatabaseColumn(String plainText) {
        if (encryptionService == null || plainText == null) {
            return plainText;
        }
        return encryptionService.encrypt(plainText);
    }

    @Override
    public String convertToEntityAttribute(String dbValue) {
        if (encryptionService == null || dbValue == null) {
            return dbValue;
        }
        try {
            return encryptionService.decrypt(dbValue);
        } catch (Exception e) {
            // Bezpieczny fallback: jeśli deszyfrowanie nie powiedzie się (np. stare dane w plaintext),
            // zwracamy surową wartość, zapobiegając awarii aplikacji.
            return dbValue;
        }
    }
}
