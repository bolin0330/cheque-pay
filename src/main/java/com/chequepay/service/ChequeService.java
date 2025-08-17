package com.chequepay.service;

import com.chequepay.dto.ChequeRequest;
import com.chequepay.dto.ChequeResponse;
import com.chequepay.dto.ChequeSplitRequest;
import com.chequepay.entity.Cheque;
import com.chequepay.repository.ChequeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChequeService {

    private final ChequeRepository chequeRepository;

    public ChequeResponse issueCheque(String payerUsername, ChequeRequest request) {
        Cheque cheque = Cheque.builder()
                .amount(request.getAmount())
                .payerUsername(payerUsername)
                .payeeUsername(request.getPayeeUsername())
                .issueDate(LocalDateTime.now())
                .expiryDate(request.getExpiryDate())
                .status("ISSUED")
                .signature("dummy-signature")
                .encryptedData("dummy-encrypted")
                .build();

        chequeRepository.save(cheque);
        return toResponse(cheque);
    }

    public Optional<ChequeResponse> getCheque(UUID chequeId) {
        return chequeRepository.findById(chequeId).map(this::toResponse);
    }

    public Optional<ChequeResponse> updateStatus(UUID chequeId, String newStatus) {
        Optional<Cheque> chequeOpt = chequeRepository.findById(chequeId);
        if (chequeOpt.isEmpty()) return Optional.empty();

        Cheque cheque = chequeOpt.get();
        cheque.setStatus(newStatus);
        chequeRepository.save(cheque);
        return Optional.of(toResponse(cheque));
    }

    public List<ChequeResponse> splitCheque(UUID chequeId, ChequeSplitRequest request) {
        Cheque parent = chequeRepository.findById(chequeId)
                .orElseThrow(() -> new RuntimeException("Cheque not found"));

        if (parent.getParentChequeId() != null) {
            throw new RuntimeException("Cannot split a child cheque");
        }

        if (!"ISSUED".equals(parent.getStatus())) {
            throw new RuntimeException("Only ISSUED cheques can be split");
        }

        BigDecimal totalSplit = request.getSplitAmounts().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalSplit.compareTo(parent.getAmount()) != 0) {
            throw new RuntimeException("Split amounts must add up to the parent cheque amount");
        }

        List<ChequeResponse> result = new ArrayList<>();
        for (BigDecimal amt : request.getSplitAmounts()) {
            Cheque child = Cheque.builder()
                    .amount(amt)
                    .payerUsername(parent.getPayerUsername())
                    .payeeUsername(parent.getPayeeUsername())
                    .issueDate(LocalDateTime.now())
                    .expiryDate(parent.getExpiryDate())
                    .status("ISSUED")
                    .parentChequeId(parent.getId())
                    .signature("dummy-signature")
                    .encryptedData("dummy-encrypted")
                    .build();
            chequeRepository.save(child);
            result.add(toResponse(child));
        }

        parent.setStatus("SPLIT");
        chequeRepository.save(parent);

        return result;
    }

    private ChequeResponse toResponse(Cheque cheque) {
        return new ChequeResponse(
                cheque.getId(),
                cheque.getAmount(),
                cheque.getPayerUsername(),
                cheque.getPayeeUsername(),
                cheque.getIssueDate(),
                cheque.getExpiryDate(),
                cheque.getStatus()
        );
    }
}
