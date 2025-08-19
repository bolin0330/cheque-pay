package com.chequepay.util;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class NonceStore {
    private static final Set<String> usedNonces = new HashSet<>();

    public static boolean hasBeenUsed(String nonce) {
        return usedNonces.contains(nonce);
    }

    public static void markAsUsed(String nonce) {
        usedNonces.add(nonce);
    }

    public static String generateNonce() {
        return UUID.randomUUID().toString();
    }
}
