package com.chequepay.util;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class NonceStore {
    private static final Set<String> usedNonces = new HashSet<>();

    public static boolean isReplay(String nonce) {
        if (usedNonces.contains(nonce)) {
            return true;
        }
        usedNonces.add(nonce);
        return false;
    }

    public static String generateNonce() {
        return UUID.randomUUID().toString();
    }
}
