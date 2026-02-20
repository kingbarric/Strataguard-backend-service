package com.strataguard.service.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.strataguard.core.config.TenantContext;
import com.strataguard.core.enums.AuditAction;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @AfterReturning(
            pointcut = "execution(* com.strataguard.service..*.create*(..)) || " +
                    "execution(* com.strataguard.service..*.update*(..)) || " +
                    "execution(* com.strataguard.service..*.delete*(..)) || " +
                    "execution(* com.strataguard.service..*.softDelete*(..))",
            returning = "result"
    )
    public void auditServiceMethod(JoinPoint joinPoint, Object result) {
        try {
            UUID tenantId = TenantContext.getTenantId();
            if (tenantId == null) return;

            String methodName = joinPoint.getSignature().getName();
            AuditAction action = resolveAction(methodName);
            String entityType = resolveEntityType(joinPoint);
            String entityId = resolveEntityId(result);

            String actorId = "system";
            String actorName = "System";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                actorId = jwt.getSubject();
                actorName = jwt.getClaimAsString("preferred_username");
                if (actorName == null) actorName = actorId;
            }

            String ipAddress = null;
            String userAgent = null;
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                ipAddress = request.getRemoteAddr();
                userAgent = request.getHeader("User-Agent");
            }

            String newValue = null;
            if (result != null && action != AuditAction.DELETE && action != AuditAction.SOFT_DELETE) {
                try {
                    newValue = objectMapper.writeValueAsString(result);
                    if (newValue.length() > 10000) {
                        newValue = newValue.substring(0, 10000) + "...(truncated)";
                    }
                } catch (Exception e) {
                    newValue = result.toString();
                }
            }

            String description = String.format("%s %s", action.name(), entityType);

            auditLogService.logEvent(tenantId, actorId, actorName, action,
                    entityType, entityId, null, newValue,
                    ipAddress, userAgent, description);

        } catch (Exception e) {
            log.warn("Failed to create audit log: {}", e.getMessage());
        }
    }

    private AuditAction resolveAction(String methodName) {
        if (methodName.startsWith("create") || methodName.startsWith("register") || methodName.startsWith("add")) {
            return AuditAction.CREATE;
        } else if (methodName.startsWith("update") || methodName.startsWith("modify") || methodName.startsWith("edit")) {
            return AuditAction.UPDATE;
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return AuditAction.SOFT_DELETE;
        }
        return AuditAction.UPDATE;
    }

    private String resolveEntityType(JoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        return className.replace("Service", "");
    }

    private String resolveEntityId(Object result) {
        if (result == null) return null;
        try {
            var idMethod = result.getClass().getMethod("getId");
            Object id = idMethod.invoke(result);
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
