package com.chequepay.service;

import com.chequepay.dto.ChequeRequest;
import com.chequepay.dto.ChequeResponse;
import com.chequepay.dto.ChequeSplitRequest;
import com.chequepay.entity.Account;
import com.chequepay.entity.Cheque;
import com.chequepay.entity.User;
import com.chequepay.repository.AccountRepository;
import com.chequepay.repository.ChequeRepository;
import com.chequepay.repository.UserRepository;
import com.chequepay.util.AESUtil;
import com.chequepay.util.NonceStore;
import com.chequepay.util.RSAUtil;
import com.chequepay.util.SignatureUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChequeService {

    private final AccountRepository accountRepository;
    private final ChequeRepository chequeRepository;
    private final UserRepository userRepository;
    private final KeyManager keyManager;

    public ChequeResponse issueCheque(String payerUsername, ChequeRequest request) {
        User payer = userRepository.findByUsername(payerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Payer not found"));
        User payee = userRepository.findByUsername(request.getPayeeUsername())
                .orElseThrow(() -> new IllegalArgumentException("Payee not found"));
        if (!payee.getRealname().equals(request.getPayeeRealname())) {
            throw new IllegalArgumentException("Payee real name does not match the account");
        }
        Account payerAccount = accountRepository.findByUsername(payerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Payer not found"));
        if (request.getAmount().compareTo(payerAccount.getBalance()) > 0) {
            throw new IllegalStateException("Cheque amount exceeds payer's account balance");
        }
        if (request.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Cheque expiry date cannot be earlier than now");
        }

        try {
            String nonce = NonceStore.generateNonce();

            String chequeData = String.format(
                    "{ \"amount\": %s, \"payer\": \"%s\", \"payee\": \"%s\", \"expiry\": \"%s\", \"nonce\": \"%s\" }",
                    request.getAmount(),
                    payerUsername,
                    request.getPayeeUsername(),
                    request.getExpiryDate(),
                    nonce
            );

            String encryptedData = AESUtil.encrypt(chequeData, keyManager.getAesKey());
            String encryptedKey = RSAUtil.encrypt(AESUtil.toBase64(keyManager.getAesKey()), keyManager.getRsaKeyPair().getPublic());
            String signature = SignatureUtil.sign(chequeData, keyManager.getRsaKeyPair().getPrivate());

            Cheque cheque = Cheque.builder()
                    .amount(request.getAmount())
                    .payerUsername(payerUsername)
                    .payerRealname(payer.getRealname())
                    .payeeUsername(request.getPayeeUsername())
                    .payeeRealname(payee.getRealname())
                    .issueDate(LocalDateTime.now())
                    .expiryDate(request.getExpiryDate())
                    .status("ISSUED")
                    .signature(signature)
                    .encryptedData(encryptedData)
                    .encryptedKey(encryptedKey)
                    .nonce(nonce)
                    .build();

            chequeRepository.save(cheque);

            return toResponse(cheque);

        } catch (Exception e) {
            throw new RuntimeException("Error issuing cheque", e);
        }
    }

    public ChequeResponse getCheque(UUID chequeId) {
        return chequeRepository.findById(chequeId).
                map(this::toResponse).
                orElseThrow(() -> new IllegalArgumentException("Cheque not found"));
    }

    public ChequeResponse updateStatus(UUID chequeId, String newStatus) {
        Cheque cheque = chequeRepository.findById(chequeId)
                .orElseThrow(() -> new IllegalArgumentException("Cheque not found"));

        cheque.setStatus(newStatus);
        chequeRepository.save(cheque);

        return toResponse(cheque);
    }

    public List<ChequeResponse> splitCheque(UUID chequeId, ChequeSplitRequest request) {
        Cheque parent = chequeRepository.findById(chequeId)
                .orElseThrow(() -> new IllegalArgumentException("Cheque not found"));

        if (parent.getParentChequeId() != null) {
            throw new IllegalStateException("Cannot split a child cheque");
        }

        if (!"ISSUED".equals(parent.getStatus())) {
            throw new IllegalStateException("Only ISSUED cheques can be split");
        }

        BigDecimal totalSplit = request.getSplitAmounts().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalSplit.compareTo(parent.getAmount()) != 0) {
            throw new IllegalStateException("Split amounts must add up to the parent cheque amount");
        }

        try {
            List<ChequeResponse> result = new ArrayList<>();
            for (BigDecimal amt : request.getSplitAmounts()) {
                String childNonce = NonceStore.generateNonce();

                String chequeData = String.format(
                        "{ \"amount\": %s, \"payer\": \"%s\", \"payee\": \"%s\", \"expiry\": \"%s\", \"nonce\": \"%s\" }",
                        amt,
                        parent.getPayerUsername(),
                        parent.getPayeeUsername(),
                        parent.getExpiryDate(),
                        childNonce
                );

                String encryptedData = AESUtil.encrypt(chequeData, keyManager.getAesKey());
                String encryptedKey = RSAUtil.encrypt(
                        AESUtil.toBase64(keyManager.getAesKey()),
                        keyManager.getRsaKeyPair().getPublic()
                );
                String signature = SignatureUtil.sign(chequeData, keyManager.getRsaKeyPair().getPrivate());

                Cheque child = Cheque.builder()
                        .amount(amt)
                        .payerUsername(parent.getPayerUsername())
                        .payerRealname(parent.getPayerRealname())
                        .payeeUsername(parent.getPayeeUsername())
                        .payeeRealname(parent.getPayeeRealname())
                        .issueDate(LocalDateTime.now())
                        .expiryDate(parent.getExpiryDate())
                        .status("ISSUED")
                        .parentChequeId(parent.getId())
                        .signature(signature)
                        .encryptedData(encryptedData)
                        .encryptedKey(encryptedKey)
                        .nonce(childNonce)
                        .build();

                chequeRepository.save(child);
                result.add(toResponse(child));
            }

            parent.setStatus("SPLIT");
            chequeRepository.save(parent);

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Error splitting cheque", e);
        }
    }

    private ChequeResponse toResponse(Cheque cheque) {
        return new ChequeResponse(
                cheque.getId(),
                cheque.getAmount(),
                cheque.getPayerUsername(),
                cheque.getPayerRealname(),
                cheque.getPayeeUsername(),
                cheque.getPayeeRealname(),
                cheque.getIssueDate(),
                cheque.getExpiryDate(),
                cheque.getStatus()
        );
    }
}
