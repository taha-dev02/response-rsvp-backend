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

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLoggingAspect {

    private final AuditService auditService;

    // ============================================================
    // ONLY LOG: USER LOGIN (SUCCESS)
    // ============================================================
    @AfterReturning(
            pointcut = "execution(* com.rsvp.controller.AuthController.login(..))",
            returning = "result"
    )
    public void logSuccessfulLogin(JoinPoint joinPoint, Object result) {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) return;

        String username = extractUsernameFromArgs(joinPoint.getArgs());
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        log.info("✅ LOGIN SUCCESS: User={} | IP={}", username, ipAddress);

        try {
            auditService.log(
                    username,
                    "LOGIN_SUCCESS",
                    "AUTH",
                    null,
                    "User logged in successfully",
                    ipAddress,
                    userAgent
            );
        } catch (Exception e) {
            log.error("Failed to save login audit", e);
        }
    }

    // ============================================================
    // ONLY LOG: USER REGISTRATION (SUCCESS)
    // ============================================================
    @AfterReturning(
            pointcut = "execution(* com.rsvp.controller.AuthController.register(..))",
            returning = "result"
    )
    public void logSuccessfulRegistration(JoinPoint joinPoint, Object result) {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) return;

        String username = extractUsernameFromArgs(joinPoint.getArgs());
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        log.info("📝 REGISTRATION SUCCESS: User={} | IP={}", username, ipAddress);

        try {
            auditService.log(
                    username,
                    "REGISTRATION_SUCCESS",
                    "AUTH",
                    null,
                    "New user registered",
                    ipAddress,
                    userAgent
            );
        } catch (Exception e) {
            log.error("Failed to save registration audit", e);
        }
    }

    // ============================================================
    // ONLY LOG: LOGIN FAILURES
    // ============================================================
    @AfterThrowing(
            pointcut = "execution(* com.rsvp.controller.AuthController.login(..))",
            throwing = "error"
    )
    public void logFailedLogin(JoinPoint joinPoint, Throwable error) {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) return;

        String username = extractUsernameFromArgs(joinPoint.getArgs());
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        log.warn("❌ LOGIN FAILED: User={} | IP={} | Error={}",
                username, ipAddress, error.getMessage());

        try {
            auditService.log(
                    username,
                    "LOGIN_FAILED",
                    "AUTH",
                    null,
                    "Login failed: " + error.getMessage(),
                    ipAddress,
                    userAgent
            );
        } catch (Exception e) {
            log.error("Failed to save failed login audit", e);
        }
    }

    // ============================================================
    // ONLY LOG: ALL CONTROLLER ERRORS
    // ============================================================
    @AfterThrowing(
            pointcut = "execution(* com.rsvp.controller..*(..))",
            throwing = "error"
    )
    public void logControllerErrors(JoinPoint joinPoint, Throwable error) {
        HttpServletRequest request = getCurrentRequest();

        String username = getUsername();
        String method = joinPoint.getSignature().getName();
        String controller = joinPoint.getSignature().getDeclaringTypeName();
        String ipAddress = request != null ? getClientIp(request) : "UNKNOWN";
        String endpoint = request != null ? request.getRequestURI() : "UNKNOWN";

        log.error("🚨 API ERROR: {}#{} | User={} | IP={} | Endpoint={} | Error={}",
                controller, method, username, ipAddress, endpoint, error.getMessage());

        try {
            auditService.log(
                    username,
                    "API_ERROR",
                    "ERROR",
                    null,
                    String.format("Error in %s: %s", endpoint, error.getMessage()),
                    ipAddress,
                    request != null ? request.getHeader("User-Agent") : null
            );
        } catch (Exception e) {
            log.error("Failed to save error audit", e);
        }
    }

    // ============================================================
    // OPTIONAL: LOG SENSITIVE DELETE OPERATIONS
    // ============================================================
    @Before("execution(* com.rsvp.service.*.deleteEvent(..)) || " +
            "execution(* com.rsvp.service.*.deleteGuest(..))")
    public void logDeleteOperations(JoinPoint joinPoint) {
        String username = getUsername();
        String operation = joinPoint.getSignature().getName();

        log.warn("🗑️ DELETE OPERATION: {} | User={}", operation, username);

        try {
            auditService.log(
                    username,
                    "DELETE_" + operation.toUpperCase(),
                    "OPERATION",
                    null,
                    "Delete operation executed",
                    null,
                    null
            );
        } catch (Exception e) {
            log.error("Failed to save delete audit", e);
        }
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
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

    private String extractUsernameFromArgs(Object[] args) {
        try {
            // Assuming first arg is LoginRequest or RegisterRequest with username
            if (args.length > 0 && args[0] != null) {
                Object request = args[0];

                // Try to get username field via reflection
                try {
                    var field = request.getClass().getDeclaredField("username");
                    field.setAccessible(true);
                    Object username = field.get(request);
                    return username != null ? username.toString() : "UNKNOWN";
                } catch (Exception e) {
                    // Fallback: try toString or just return UNKNOWN
                    return "UNKNOWN";
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract username from args", e);
        }
        return "UNKNOWN";
    }
}