package com.chequepay.service;

import com.chequepay.util.AESUtil;
import com.chequepay.util.RSAUtil;
import lombok.Getter;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.KeyPair;

@Component
@Getter
public class KeyManager {

    private final KeyPair rsaKeyPair;
    private final SecretKey aesKey;

    public KeyManager() {
        try {
            this.rsaKeyPair = RSAUtil.generateKeyPair(2048);
            this.aesKey = AESUtil.generateAESKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize KeyManager", e);
        }
    }
}
