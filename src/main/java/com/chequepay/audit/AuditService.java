package com.chequepay.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void recordEvent(String eventType, String username, String details, String ipAddress, String status) {
        AuditLog log = AuditLog.builder()
                .eventType(eventType)
                .username(username)
                .details(MaskingUtil.maskSensitive(details))
                .ipAddress(ipAddress)
                .timestamp(LocalDateTime.now())
                .status(status)
                .build();
        auditLogRepository.save(log);
    }
}
