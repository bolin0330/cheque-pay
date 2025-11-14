package com.chequepay;

import com.chequepay.audit.*;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class AuditServiceTest {

    @Test
    void shouldSaveAuditLogWithMaskedFields() {

        AuditLogRepository repo = mock(AuditLogRepository.class);
        AuditService service = new AuditService(repo);

        String details = """
                { "username": "yamarze", "password": "secret123", "phoneNumber": "0912345678" }
                """;

        service.recordEvent(
                "LOGIN",
                "yamarze",
                details,
                "127.0.0.1",
                "SUCCESS"
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repo, times(1)).save(captor.capture());

        AuditLog saved = captor.getValue();

        assertEquals("LOGIN", saved.getEventType());
        assertEquals("yamarze", saved.getUsername());
        assertEquals("127.0.0.1", saved.getIpAddress());
        assertEquals("SUCCESS", saved.getStatus());
        assertNotNull(saved.getTimestamp());

        assertFalse(saved.getDetails().contains("secret123"));
        assertFalse(saved.getDetails().contains("0912345678"));

        assertTrue(saved.getDetails().contains("\"password\":\"***\""));
        assertTrue(saved.getDetails().contains("\"phoneNumber\":\"***\""));
    }

    @Test
    void shouldReplacePasswordAndPhone() {

        String json = """
                {
                    "email": "abc@example.com",
                    "password": "mypassword",
                    "phoneNumber": "0987654321",
                    "encryptedKey": "ABC123"
                }
                """;

        String masked = MaskingUtil.maskSensitive(json);

        assertTrue(masked.contains("\"password\":\"***\""));
        assertTrue(masked.contains("\"phoneNumber\":\"***\""));
        assertTrue(masked.contains("\"email\":\"***\""));
        assertTrue(masked.contains("\"encryptedKey\":\"***\""));

        assertFalse(masked.contains("mypassword"));
        assertFalse(masked.contains("0987654321"));
        assertFalse(masked.contains("ABC123"));
    }

    @Test
    void shouldCallAuditService() {

        AuditService auditService = mock(AuditService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getUserPrincipal()).thenReturn(null);

        AuditAspect aspect = new AuditAspect(auditService, request);

        JoinPoint joinPoint = mock(JoinPoint.class);
        MethodSignature ms = mock(MethodSignature.class);

        when(joinPoint.getSignature()).thenReturn(ms);
        when(ms.toShortString()).thenReturn("SomeController.someMethod()");

        aspect.logSuccess(joinPoint, "OK");

        verify(auditService).recordEvent(
                eq("API_CALL"),
                eq("ANONYMOUS"),
                contains("SomeController.someMethod() -> OK"),
                eq("127.0.0.1"),
                eq("SUCCESS")
        );
    }
}
