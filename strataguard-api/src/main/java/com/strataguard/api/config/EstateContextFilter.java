package com.strataguard.api.config;

import com.strataguard.core.config.EstateContext;
import com.strataguard.core.config.TenantContext;
import com.strataguard.core.entity.EstateMembership;
import com.strataguard.core.enums.UserRole;
import com.strataguard.infrastructure.repository.EstateMembershipRepository;
import com.strataguard.infrastructure.repository.EstateRepository;
import com.strataguard.service.permission.PermissionResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class EstateContextFilter extends OncePerRequestFilter {

    private static final String ESTATE_HEADER = "X-ESTATE-ID";

    private final EstateMembershipRepository membershipRepository;
    private final EstateRepository estateRepository;
    private final PermissionResolver permissionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String estateIdHeader = request.getHeader(ESTATE_HEADER);

            if (estateIdHeader != null && !estateIdHeader.isBlank()) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                    String userId = jwt.getSubject();
                    UUID estateId;

                    try {
                        estateId = UUID.fromString(estateIdHeader.trim());
                    } catch (IllegalArgumentException e) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"success\":false,\"message\":\"Invalid X-ESTATE-ID format\"}");
                        return;
                    }

                    boolean isSuperAdmin = auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(a -> a.equals("ROLE_SUPER_ADMIN"));

                    if (isSuperAdmin) {
                        UUID tenantId = TenantContext.getTenantId();
                        if (tenantId != null) {
                            boolean estateExists = estateRepository
                                .findByIdAndTenantId(estateId, tenantId).isPresent();
                            if (!estateExists) {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.setContentType("application/json");
                                response.getWriter().write(
                                    "{\"success\":false,\"message\":\"Estate not found in tenant\"}");
                                return;
                            }
                        }
                        EstateContext.setEstateId(estateId);
                        EstateContext.setUserId(userId);
                        EstateContext.setRole("SUPER_ADMIN");
                        Set<String> allPerms = Set.copyOf(
                            permissionResolver.getDefaultPermissions(UserRole.SUPER_ADMIN));
                        EstateContext.setPermissions(allPerms);
                    } else {
                        Optional<EstateMembership> membershipOpt =
                            membershipRepository.findActiveByUserIdAndEstateId(userId, estateId);

                        if (membershipOpt.isEmpty()) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                "{\"success\":false,\"message\":\"No active membership for this estate\"}");
                            return;
                        }

                        EstateMembership membership = membershipOpt.get();

                        UUID tenantId = TenantContext.getTenantId();
                        if (tenantId != null && !membership.getTenantId().equals(tenantId)) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                "{\"success\":false,\"message\":\"Tenant mismatch\"}");
                            return;
                        }

                        Set<String> permissions = permissionResolver.resolvePermissions(membership);

                        EstateContext.setEstateId(estateId);
                        EstateContext.setUserId(userId);
                        EstateContext.setRole(membership.getRole().name());
                        EstateContext.setPermissions(permissions);
                    }
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            EstateContext.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/api/v1/auth")
                || path.startsWith("/api/v1/payments/webhook")
                || path.startsWith("/ws")
                || path.equals("/health");
    }
}
