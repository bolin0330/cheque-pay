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

    public boolean verifyCheque(UUID chequeId) {
        Optional<Cheque> chequeOpt = chequeRepository.findById(chequeId);
        if (chequeOpt.isEmpty()) return false;

        Cheque cheque = chequeOpt.get();

        if (!"ISSUED".equals(cheque.getStatus())) return false;
        if (cheque.getExpiryDate().isBefore(LocalDateTime.now())) return false;

        try {
            String aesKeyBase64 = RSAUtil.decrypt(cheque.getEncryptedKey(), keyManager.getRsaKeyPair().getPrivate());
            SecretKey aesKey = AESUtil.fromBase64(aesKeyBase64);
            String chequeData = AESUtil.decrypt(cheque.getEncryptedData(), aesKey);

            boolean valid = SignatureUtil.verify(chequeData, cheque.getSignature(), keyManager.getRsaKeyPair().getPublic());
            if (!valid) return false;
            if (NonceStore.hasBeenUsed(cheque.getNonce())) return false;

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean settleCheque(UUID chequeId) {
        Optional<Cheque> chequeOpt = chequeRepository.findById(chequeId);
        if (chequeOpt.isEmpty()) return false;

        Cheque cheque = chequeOpt.get();

        if (!verifyCheque(chequeId)) return false;
        if (cheque.getExpiryDate().isBefore(LocalDateTime.now())) return false;

        try {
            Account payer = accountRepository.findByUsername(cheque.getPayerUsername())
                    .orElseThrow(() -> new RuntimeException("Payer account not found"));
            Account payee = accountRepository.findByUsername(cheque.getPayeeUsername())
                    .orElseThrow(() -> new RuntimeException("Payee account not found"));

            if (payer.getBalance().compareTo(cheque.getAmount()) < 0) {
                throw new RuntimeException("Insufficient balance");
            }

            payer.setBalance(payer.getBalance().subtract(cheque.getAmount()));
            payee.setBalance(payee.getBalance().add(cheque.getAmount()));
            accountRepository.save(payer);
            accountRepository.save(payee);

            cheque.setStatus("CLEARED");
            chequeRepository.save(cheque);
            NonceStore.markAsUsed(cheque.getNonce());

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
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
