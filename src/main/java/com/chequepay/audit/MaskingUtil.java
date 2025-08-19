package com.chequepay.audit;

public class MaskingUtil {

    public static String maskSensitive(String input) {
        if (input == null) return null;

        return input
                .replaceAll("(?i)\"password\"\\s*:\\s*\".*?\"", "\"password\":\"***\"")
                .replaceAll("(?i)\"email\"\\s*:\\s*\".*?\"", "\"email\":\"***\"")
                .replaceAll("(?i)\"phoneNumber\"\\s*:\\s*\".*?\"", "\"phoneNumber\":\"***\"")
                .replaceAll("(?i)\"encryptedKey\"\\s*:\\s*\".*?\"", "\"encryptedKey\":\"***\"");
    }
}
