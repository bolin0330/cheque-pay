package com.chequepay;

import com.chequepay.util.AESUtil;
import com.chequepay.util.NonceStore;
import com.chequepay.util.RSAUtil;
import com.chequepay.util.SignatureUtil;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.*;

public class EncryptionUtilTest {

    @Test
    void testAES() throws Exception {
        String originalText = "This is a test message for AES encryption.";
        SecretKey aesKey = AESUtil.generateAESKey();

        String encrypted = AESUtil.encrypt(originalText, aesKey);
        String decrypted = AESUtil.decrypt(encrypted, aesKey);

        assertNotNull(encrypted, "Encrypted text should not be null");
        assertNotEquals(originalText, encrypted, "Encrypted text should not be the same as plain text");
        assertEquals(originalText, decrypted, "Decrypted text must equal the original text");
    }

    @Test
    void testRSA() throws Exception {
        KeyPair keyPair = RSAUtil.generateKeyPair(2048);
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        SecretKey aesKey = AESUtil.generateAESKey();
        String aesKeyBase64 = AESUtil.toBase64(aesKey);

        String encryptedAESKey = RSAUtil.encrypt(aesKeyBase64, publicKey);
        String decryptedAESKey = RSAUtil.decrypt(encryptedAESKey, privateKey);
        SecretKey recoveredKey = AESUtil.fromBase64(decryptedAESKey);

        assertNotNull(encryptedAESKey);
        assertNotEquals(aesKeyBase64, encryptedAESKey, "RSA encrypted key must differ from plain AES key");
        assertEquals(aesKeyBase64, decryptedAESKey, "Decrypted AES key must match original");

        assertArrayEquals(aesKey.getEncoded(), recoveredKey.getEncoded(), "Recovered AES key must be identical");
    }

    @Test
    void testSignature() throws Exception {
        KeyPair keyPair = RSAUtil.generateKeyPair(2048);
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        String data = "Cheque JSON Data ABC123";

        String signature = SignatureUtil.sign(data, privateKey);
        boolean isValid = SignatureUtil.verify(data, signature, publicKey);

        assertNotNull(signature, "Signature must not be null");
        assertTrue(isValid, "Signature should be valid for the original data");
    }

    @Test
    void testSignatureFailsOnTamperedData() throws Exception {
        KeyPair keyPair = RSAUtil.generateKeyPair(2048);
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        String originalData = "This is the original cheque data";
        String fakeData = "This is the FAKE cheque data";

        String signature = SignatureUtil.sign(originalData, privateKey);
        boolean shouldFail = SignatureUtil.verify(fakeData, signature, publicKey);

        assertFalse(shouldFail, "Signature verification should fail for tampered data");
    }

    @Test
    void testNonce() {
        String nonce = NonceStore.generateNonce();

        assertNotNull(nonce, "Nonce should not be null");
        assertFalse(NonceStore.hasBeenUsed(nonce), "Nonce should not be marked as used initially");

        NonceStore.markAsUsed(nonce);
        assertTrue(NonceStore.hasBeenUsed(nonce), "Nonce should be marked as used after calling markAsUsed()");
        assertTrue(NonceStore.hasBeenUsed(nonce), "Replay detection: Nonce should still be marked as used");
    }
}
