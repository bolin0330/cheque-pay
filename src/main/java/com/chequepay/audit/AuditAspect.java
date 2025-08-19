package com.chequepay.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;
    private final HttpServletRequest request;

    @AfterReturning(value = "execution(* com.chequepay.controller..*(..))", returning = "result")
    public void logSuccess(JoinPoint joinPoint, Object result) {
        String username = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "ANONYMOUS";
        auditService.recordEvent(
                "API_CALL",
                username,
                joinPoint.getSignature().toShortString() + " -> " + result,
                request.getRemoteAddr(),
                "SUCCESS"
        );
    }

    @AfterThrowing(value = "execution(* com.chequepay.controller..*(..))", throwing = "ex")
    public void logFailure(JoinPoint joinPoint, Throwable ex) {
        String username = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "ANONYMOUS";
        auditService.recordEvent(
                "API_CALL",
                username,
                joinPoint.getSignature().toShortString() + " -> " + ex.getMessage(),
                request.getRemoteAddr(),
                "FAILURE"
        );
    }
}
