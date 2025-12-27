package com.rsvp.config;

import com.rsvp.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor  // Add this for dependency injection
public class AuditLoggingAspect {

    private final AuditService auditService;  // Add this

    @Pointcut("execution(* com.rsvp.controller..*(..))")
    public void controllerMethods() {}

    @Before("controllerMethods()")
    public void logControllerBefore(JoinPoint joinPoint) {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            String username = getUsername();
            String method = joinPoint.getSignature().getName();
            String ipAddress = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");

            // Log to console
            log.info("API Request: {} {} | Method: {} | User: {} | IP: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    method,
                    username,
                    ipAddress
            );

            // Save to database
            try {
                auditService.log(
                        username,
                        request.getMethod() + "_" + method.toUpperCase(),
                        "API",
                        null,
                        request.getRequestURI(),
                        ipAddress,
                        userAgent
                );
            } catch (Exception e) {
                log.error("Failed to save audit log", e);
            }
        }
    }

    @AfterReturning(pointcut = "controllerMethods()", returning = "result")
    public void logControllerAfterReturning(JoinPoint joinPoint, Object result) {
        log.info("API Response: {} | Status: Success",
                joinPoint.getSignature().getName()
        );

        // Save success to database
        try {
            auditService.log(
                    getUsername(),
                    "SUCCESS_" + joinPoint.getSignature().getName().toUpperCase(),
                    "API",
                    null,
                    "Operation completed successfully",
                    null,
                    null
            );
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }

    @AfterThrowing(pointcut = "controllerMethods()", throwing = "error")
    public void logControllerAfterThrowing(JoinPoint joinPoint, Throwable error) {
        HttpServletRequest request = getCurrentRequest();
        String username = getUsername();
        String ipAddress = request != null ? getClientIp(request) : "UNKNOWN";

        log.error("API Error: {} {} | Method: {} | Error: {} | User: {} | IP: {}",
                request != null ? request.getMethod() : "UNKNOWN",
                request != null ? request.getRequestURI() : "UNKNOWN",
                joinPoint.getSignature().getName(),
                error.getMessage(),
                username,
                ipAddress
        );

        // Save error to database
        try {
            auditService.log(
                    username,
                    "ERROR_" + joinPoint.getSignature().getName().toUpperCase(),
                    "API",
                    null,
                    "Error: " + error.getMessage(),
                    ipAddress,
                    request != null ? request.getHeader("User-Agent") : null
            );
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }

    @Before("execution(* com.rsvp.service.*.delete*(..)) || " +
            "execution(* com.rsvp.service.*.import*(..))")
    public void logSensitiveOperations(JoinPoint joinPoint) {
        String username = getUsername();
        String operation = joinPoint.getSignature().getName();

        log.warn("SENSITIVE OPERATION: {} | Args: {} | User: {} | Time: {}",
                operation,
                Arrays.toString(joinPoint.getArgs()),
                username,
                LocalDateTime.now()
        );

        // Save sensitive operation to database
        try {
            auditService.log(
                    username,
                    "SENSITIVE_" + operation.toUpperCase(),
                    "OPERATION",
                    null,
                    "Args: " + Arrays.toString(joinPoint.getArgs()),
                    null,
                    null
            );
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String getUsername() {
        try {
            return org.springframework.security.core.context.SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getName();
        } catch (Exception e) {
            return "anonymousUser";
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}