package com.chequepay;

import com.chequepay.entity.Cheque;
import com.chequepay.repository.ChequeRepository;
import com.chequepay.service.ClearingService;
import com.chequepay.service.KeyManager;
import com.chequepay.util.AESUtil;
import com.chequepay.util.NonceStore;
import com.chequepay.util.RSAUtil;

import com.chequepay.util.SignatureUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.security.KeyPair;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class VerifyServiceTest {

    @InjectMocks
    private ClearingService clearingService;

    @Mock
    private ChequeRepository chequeRepository;

    @Mock
    private KeyManager keyManager;

    private Cheque cheque;
    private UUID chequeId;
    private KeyPair rsaKeyPair;

    @BeforeEach
    void setUp() throws Exception {
        chequeId = UUID.randomUUID();

        rsaKeyPair = RSAUtil.generateKeyPair(2048);
        SecretKey aesKey = AESUtil.generateAESKey();
        String nonce = NonceStore.generateNonce();

        String chequeData = "{ \"amount\": 2000, \"payer\": \"bolin0330\", \"payee\": \"one_rakugaki\n\", \"expiry\": \"2030-12-31T23:59\", \"nonce\": \""
                + nonce + "\" }";

        String encryptedData = AESUtil.encrypt(chequeData, aesKey);
        String encryptedKey = RSAUtil.encrypt(AESUtil.toBase64(aesKey), rsaKeyPair.getPublic());
        String signature = SignatureUtil.sign(chequeData, rsaKeyPair.getPrivate());

        cheque = Cheque.builder()
                .id(chequeId)
                .amount(BigDecimal.valueOf(2000))
                .payerUsername("bolin0330")
                .payeeUsername("one_rakugaki")
                .expiryDate(LocalDateTime.now().plusDays(10))
                .status("ISSUED")
                .encryptedData(encryptedData)
                .encryptedKey(encryptedKey)
                .signature(signature)
                .nonce(nonce)
                .build();

        Mockito.when(chequeRepository.findById(chequeId)).thenReturn(Optional.of(cheque));
    }

    @Test
    void verifySuccess() {
        Mockito.when(keyManager.getRsaKeyPair()).thenReturn(rsaKeyPair);

        assertDoesNotThrow(() -> clearingService.verifyCheque(chequeId, "one_rakugaki"));
    }

    @Test
    void verifyChequeNotFound() {
        Mockito.when(chequeRepository.findById(chequeId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> clearingService.verifyCheque(chequeId, "one_rakugaki"));
        assertEquals("Cheque not found", ex.getMessage());
    }

    @Test
    void verifyInvalidStatus() {
        cheque.setStatus("CLEARED");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> clearingService.verifyCheque(chequeId, "one_rakugaki"));
        assertEquals("Cheque is not in ISSUED status", ex.getMessage());
    }

    @Test
    void verifyChequeExpired() {
        cheque.setExpiryDate(LocalDateTime.now().minusDays(10));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> clearingService.verifyCheque(chequeId, "one_rakugaki"));
        assertEquals("Cheque has expired", ex.getMessage());
    }

    @Test
    void verifyWrongUser() {
        Mockito.when(chequeRepository.findById(chequeId)).thenReturn(Optional.of(cheque));

        SecurityException ex = assertThrows(SecurityException.class,
                () -> clearingService.verifyCheque(chequeId, "bolin0330"));
        assertEquals("You are not authorized to settle this cheque.", ex.getMessage());
    }

    @Test
    void verifyNonceAlreadyUsed() {
        Mockito.when(keyManager.getRsaKeyPair()).thenReturn(rsaKeyPair);
        NonceStore.markAsUsed(cheque.getNonce());

        SecurityException ex = assertThrows(SecurityException.class,
                () -> clearingService.verifyCheque(chequeId, "one_rakugaki"));
        assertEquals("Cheque nonce has already been used", ex.getMessage());
    }

    @Test
    void verifyInvalidSignature() {
        Mockito.when(keyManager.getRsaKeyPair()).thenReturn(rsaKeyPair);

        byte[] fakeSig = new byte[256];
        new java.security.SecureRandom().nextBytes(fakeSig);
        cheque.setSignature(Base64.getEncoder().encodeToString(fakeSig));

        SecurityException ex = assertThrows(SecurityException.class,
                () -> clearingService.verifyCheque(chequeId, "one_rakugaki"));
        assertEquals("Invalid cheque signature", ex.getMessage());
    }
}
