package com.chequepay.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cheques")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cheque {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String payerUsername;

    @Column(nullable = false)
    private String payeeUsername;

    @Column(nullable = false, length = 25)
    private String payerRealname;

    @Column(nullable = false, length = 25)
    private String payeeRealname;

    @Column(nullable = false)
    private LocalDateTime issueDate;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private String status;

    @Column(length = 5000)
    private String signature;

    @Column(length = 5000)
    private String encryptedData;

    @Column(length = 5000)
    private String encryptedKey;

    private UUID parentChequeId;

    @Column(unique = true, nullable = false)
    private String nonce;
}