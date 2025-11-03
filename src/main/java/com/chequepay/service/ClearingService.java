package com.chequepay.service;

import com.chequepay.entity.Account;
import com.chequepay.entity.Cheque;
import com.chequepay.repository.AccountRepository;
import com.chequepay.repository.ChequeRepository;
import com.chequepay.util.AESUtil;
import com.chequepay.util.RSAUtil;
import com.chequepay.util.NonceStore;
import com.chequepay.util.SignatureUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ClearingService {

    private final ChequeRepository chequeRepository;
    private final AccountRepository accountRepository;
    private final KeyManager keyManager;

    public void verifyCheque(UUID chequeId, String currentUser) {
        Cheque cheque = chequeRepository.findById(chequeId)
                .orElseThrow(() -> new IllegalArgumentException("Cheque not found"));

        if (!"ISSUED".equals(cheque.getStatus())) {
            throw new IllegalStateException("Cheque is not in ISSUED status");
        }

        if (cheque.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Cheque has expired");
        }

        if (!cheque.getPayeeUsername().equals(currentUser)) {
            throw new SecurityException("You are not authorized to settle this cheque.");
        }

        try {
            String aesKeyBase64 = RSAUtil.decrypt(cheque.getEncryptedKey(), keyManager.getRsaKeyPair().getPrivate());
            SecretKey aesKey = AESUtil.fromBase64(aesKeyBase64);
            String chequeData = AESUtil.decrypt(cheque.getEncryptedData(), aesKey);

            boolean valid = SignatureUtil.verify(chequeData, cheque.getSignature(), keyManager.getRsaKeyPair().getPublic());
            if (!valid) {
                throw new SecurityException("Invalid cheque signature");
            }
            if (NonceStore.hasBeenUsed(cheque.getNonce())) {
                throw new SecurityException("Cheque nonce has already been used");
            }

        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error verifying cheque", e);
        }
    }

    public void settleCheque(UUID chequeId, String currentUser) {
        Cheque cheque = chequeRepository.findById(chequeId)
                .orElseThrow(() -> new IllegalArgumentException("Cheque not found"));

        verifyCheque(chequeId, currentUser);

        try {
            Account payer = accountRepository.findByUsername(cheque.getPayerUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Payer account not found"));
            Account payee = accountRepository.findByUsername(cheque.getPayeeUsername())
                    .orElseThrow(() -> new IllegalArgumentException("Payee account not found"));

            if (payer.getBalance().compareTo(cheque.getAmount()) < 0) {
                throw new IllegalStateException("Insufficient balance");
            }

            payer.setBalance(payer.getBalance().subtract(cheque.getAmount()));
            payee.setBalance(payee.getBalance().add(cheque.getAmount()));
            accountRepository.save(payer);
            accountRepository.save(payee);

            cheque.setStatus("CLEARED");
            chequeRepository.save(cheque);
            NonceStore.markAsUsed(cheque.getNonce());

        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error settling cheque", e);
        }
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void expireOldCheques() {
        List<Cheque> cheques = chequeRepository.findAll();
        for (Cheque cheque : cheques) {
            if ("ISSUED".equals(cheque.getStatus()) &&
                    cheque.getExpiryDate().isBefore(LocalDateTime.now())) {
                cheque.setStatus("EXPIRED");
                chequeRepository.save(cheque);
            }
        }
    }
}
