package com.chequepay;

import com.chequepay.entity.Account;
import com.chequepay.entity.Cheque;
import com.chequepay.repository.AccountRepository;
import com.chequepay.repository.ChequeRepository;
import com.chequepay.service.ClearingService;
import com.chequepay.service.KeyManager;
import com.chequepay.util.AESUtil;
import com.chequepay.util.NonceStore;
import com.chequepay.util.RSAUtil;
import com.chequepay.util.SignatureUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.security.KeyPair;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SettleServiceTest {

    @InjectMocks
    private ClearingService clearingService;

    @Mock
    private ChequeRepository chequeRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private KeyManager keyManager;

    private Cheque cheque;
    private UUID chequeId;

    @BeforeEach
    void setUp() throws Exception {
        chequeId = UUID.randomUUID();
        KeyPair rsaKeyPair = RSAUtil.generateKeyPair(2048);

        SecretKey aesKey = AESUtil.generateAESKey();
        String nonce = NonceStore.generateNonce();

        String chequeData = "{ \"amount\": 3000, \"payer\": \"min9yu_k\", \"payee\": \"larissalambert\", \"expiry\": \"2030-12-31T23:59\", \"nonce\": \""
                + nonce + "\" }";
        String encryptedData = AESUtil.encrypt(chequeData, aesKey);
        String encryptedKey = RSAUtil.encrypt(AESUtil.toBase64(aesKey), rsaKeyPair.getPublic());
        String signature = SignatureUtil.sign(chequeData, rsaKeyPair.getPrivate());

        cheque = Cheque.builder()
                .id(chequeId)
                .amount(BigDecimal.valueOf(3000))
                .payerUsername("min9yu_k")
                .payeeUsername("larissalambert")
                .expiryDate(LocalDateTime.now().plusDays(10))
                .status("ISSUED")
                .encryptedData(encryptedData)
                .encryptedKey(encryptedKey)
                .signature(signature)
                .nonce(nonce)
                .build();

        lenient().when(chequeRepository.findById(chequeId)).thenReturn(Optional.of(cheque));
        lenient().when(keyManager.getRsaKeyPair()).thenReturn(rsaKeyPair);
    }

    @Test
    void settleSuccess() {
        Account payer = Account.builder().username("min9yu_k").realname("Mingyu Kim").balance(BigDecimal.valueOf(5000)).build();
        Account payee = Account.builder().username("larissalambert").realname("Larissa Lambert").balance(BigDecimal.valueOf(1000)).build();
        Mockito.when(accountRepository.findByUsername("min9yu_k")).thenReturn(Optional.of(payer));
        Mockito.when(accountRepository.findByUsername("larissalambert")).thenReturn(Optional.of(payee));

        assertDoesNotThrow(() -> clearingService.settleCheque(chequeId, "larissalambert"));

        assertEquals(BigDecimal.valueOf(2000), payer.getBalance());
        assertEquals(BigDecimal.valueOf(4000), payee.getBalance());
        assertEquals("CLEARED", cheque.getStatus());
        assertTrue(NonceStore.hasBeenUsed(cheque.getNonce()));
    }

    @Test
    void settleMissingPayer() {
        Mockito.when(accountRepository.findByUsername("min9yu_k")).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> clearingService.settleCheque(chequeId, "larissalambert"));
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
        assertEquals("Payer account not found", ex.getCause().getMessage());
    }

    @Test
    void settleMissingPayee() {
        Mockito.when(accountRepository.findByUsername("min9yu_k")).thenReturn(Optional.of(Account.builder().username("min9yu_k").realname("Mingyu Kim").balance(BigDecimal.valueOf(5000)).build()));
        Mockito.when(accountRepository.findByUsername("larissalambert")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> clearingService.settleCheque(chequeId, "larissalambert"));
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
        assertEquals("Payee account not found", ex.getCause().getMessage());
    }

    @Test
    void settleInsufficientBalance() {
        Account payer = Account.builder().username("min9yu_k").realname("Mingyu Kim").balance(BigDecimal.valueOf(50)).build();
        Account payee = Account.builder().username("larissalambert").realname("Larissa Lambert").balance(BigDecimal.valueOf(5000)).build();
        Mockito.when(accountRepository.findByUsername("min9yu_k")).thenReturn(Optional.of(payer));
        Mockito.when(accountRepository.findByUsername("larissalambert")).thenReturn(Optional.of(payee));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> clearingService.settleCheque(chequeId, "larissalambert"));
        assertInstanceOf(IllegalStateException.class, ex.getCause());
        assertEquals("Insufficient balance", ex.getCause().getMessage());
    }

    @Test
    void settleChequeNotFound() {
        UUID fakeId = UUID.randomUUID();

        Mockito.when(chequeRepository.findById(fakeId)).thenReturn(Optional.empty());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> clearingService.settleCheque(fakeId, "larissalambert"));
        assertEquals("Cheque not found", ex.getMessage());
    }

    @Test
    void settleInvalidStatus() {
        cheque.setStatus("VOIDED");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> clearingService.verifyCheque(chequeId, "larissalambert"));
        assertEquals("Cheque is not in ISSUED status", ex.getMessage());
    }

    @Test
    void settleChequeExpired() {
        cheque.setExpiryDate(LocalDateTime.now().minusDays(10));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> clearingService.verifyCheque(chequeId, "larissalambert"));
        assertEquals("Cheque has expired", ex.getMessage());
    }

    @Test
    void settleWrongUser() {
        SecurityException ex = assertThrows(SecurityException.class,
                () -> clearingService.settleCheque(chequeId, "someoneElse"));
        assertEquals("You are not authorized to settle this cheque.", ex.getMessage());
    }

    @Test
    void settleUnexpectedException() {
        Mockito.when(accountRepository.findByUsername("min9yu_k")).thenThrow(new RuntimeException("DB down"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> clearingService.settleCheque(chequeId, "larissalambert"));
        assertEquals("Error settling cheque", ex.getMessage());
    }

}
