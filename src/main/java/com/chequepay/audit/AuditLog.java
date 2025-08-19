package com.chequepay.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue
    private UUID id;

    private String eventType;
    private String username;

    @Column(columnDefinition = "TEXT")
    private String details;

    private LocalDateTime timestamp;
    private String ipAddress;
    private String status;
}
