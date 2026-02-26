package com.strataguard.api.config;

import com.strataguard.core.config.EstateContext;
import com.strataguard.core.config.TenantContext;
import com.strataguard.core.entity.EstateMembership;
import com.strataguard.core.enums.MembershipStatus;
import com.strataguard.core.enums.UserRole;
import com.strataguard.infrastructure.repository.EstateMembershipRepository;
import com.strataguard.infrastructure.repository.EstateRepository;
import com.strataguard.service.permission.PermissionResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EstateContextFilter — RBAC filter chain")
class EstateContextFilterTest {

    private static final UUID ESTATE_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ESTATE_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID TENANT_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String USER_ID = "keycloak-user-123";

    @Mock private EstateMembershipRepository membershipRepository;
    @Mock private EstateRepository estateRepository;
    @Mock private PermissionResolver permissionResolver;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private EstateContextFilter filter;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
        EstateContext.clear();
    }

    // ── Helpers ──

    private Jwt buildJwt(String userId) {
        return Jwt.withTokenValue("mock-token")
            .header("alg", "RS256")
            .subject(userId)
            .claim("tenant_id", TENANT_1.toString())
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
    }

    private void setSecurityContext(String userId, String... roles) {
        Jwt jwt = buildJwt(userId);
        List<SimpleGrantedAuthority> authorities = Arrays.stream(roles)
            .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
            .toList();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, authorities, userId);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private EstateMembership buildMembership(UUID estateId, UserRole role, UUID tenantId) {
        EstateMembership m = new EstateMembership();
        m.setId(UUID.randomUUID());
        m.setUserId(USER_ID);
        m.setEstateId(estateId);
        m.setRole(role);
        m.setStatus(MembershipStatus.ACTIVE);
        m.setTenantId(tenantId);
        return m;
    }

    // ═══════════════════════════════════════════════════════
    // 1. Cross-estate denial
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("(1) Cross-estate denial")
    class CrossEstateDenial {

        @Test
        @DisplayName("user with membership in Estate A is denied access to Estate B")
        void deniedAccessToUnmemberedEstate() throws Exception {
            setSecurityContext(USER_ID, "ESTATE_ADMIN");
            TenantContext.setTenantId(TENANT_1);

            // User has membership in Estate A but NOT Estate B
            when(membershipRepository.findActiveByUserIdAndEstateId(USER_ID, ESTATE_B))
                .thenReturn(Optional.empty());

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-ESTATE-ID", ESTATE_B.toString());
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
            assertThat(response.getContentAsString()).contains("No active membership");
            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("user with membership in Estate A can access Estate A")
        void allowedAccessToOwnEstate() throws Exception {
            setSecurityContext(USER_ID, "ESTATE_ADMIN");
            TenantContext.setTenantId(TENANT_1);

            EstateMembership membership = buildMembership(ESTATE_A, UserRole.ESTATE_ADMIN, TENANT_1);
            when(membershipRepository.findActiveByUserIdAndEstateId(USER_ID, ESTATE_A))
                .thenReturn(Optional.of(membership));
            when(permissionResolver.resolvePermissions(membership))
                .thenReturn(Set.of("estate.read", "estate.update"));

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-ESTATE-ID", ESTATE_A.toString());
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("tenant mismatch between JWT and membership is denied")
        void tenantMismatchDenied() throws Exception {
            setSecurityContext(USER_ID, "ESTATE_ADMIN");
            TenantContext.setTenantId(TENANT_1);

            // Membership belongs to a different tenant
            EstateMembership membership = buildMembership(ESTATE_A, UserRole.ESTATE_ADMIN, TENANT_2);
            when(membershipRepository.findActiveByUserIdAndEstateId(USER_ID, ESTATE_A))
                .thenReturn(Optional.of(membership));

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-ESTATE-ID", ESTATE_A.toString());
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
            assertThat(response.getContentAsString()).contains("Tenant mismatch");
            verify(filterChain, never()).doFilter(any(), any());
        }
    }

    // ═══════════════════════════════════════════════════════
    // 2. Permission downgrade — immediate effect in filter
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("(2) Permission downgrade — immediate effect via filter")
    class PermissionDowngradeFilter {

        @Test
        @DisplayName("resolved permissions change when underlying data changes between requests")
        void permissionChangeReflected() throws Exception {
            setSecurityContext(USER_ID, "ESTATE_ADMIN");
            TenantContext.setTenantId(TENANT_1);

            EstateMembership membership = buildMembership(ESTATE_A, UserRole.ESTATE_ADMIN, TENANT_1);
            when(membershipRepository.findActiveByUserIdAndEstateId(USER_ID, ESTATE_A))
                .thenReturn(Optional.of(membership));

            // Request 1: has resident.create
            when(permissionResolver.resolvePermissions(membership))
                .thenReturn(Set.of("estate.read", "resident.create"));

            MockHttpServletRequest req1 = new MockHttpServletRequest();
            req1.addHeader("X-ESTATE-ID", ESTATE_A.toString());
            MockHttpServletResponse res1 = new MockHttpServletResponse();

            // Capture permissions during filterChain execution
            Set<String> permsReq1 = new HashSet<>();
            doAnswer(inv -> {
                permsReq1.addAll(EstateContext.getPermissions());
                return null;
            }).when(filterChain).doFilter(req1, res1);

            filter.doFilterInternal(req1, res1, filterChain);
            assertThat(permsReq1).contains("resident.create");

            // Request 2: resident.create revoked
            when(permissionResolver.resolvePermissions(membership))
                .thenReturn(Set.of("estate.read"));

            MockHttpServletRequest req2 = new MockHttpServletRequest();
            req2.addHeader("X-ESTATE-ID", ESTATE_A.toString());
            MockHttpServletResponse res2 = new MockHttpServletResponse();

            Set<String> permsReq2 = new HashSet<>();
            doAnswer(inv -> {
                permsReq2.addAll(EstateContext.getPermissions());
                return null;
            }).when(filterChain).doFilter(req2, res2);

            filter.doFilterInternal(req2, res2, filterChain);
            assertThat(permsReq2).doesNotContain("resident.create");
            assertThat(permsReq2).contains("estate.read");
        }
    }

    // ═══════════════════════════════════════════════════════
    // 3. Multi-estate user — different roles per estate
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("(3) Multi-estate user — different roles per estate")
    class MultiEstateUser {

        @Test
        @DisplayName("same user gets ESTATE_ADMIN perms in Estate A and SECURITY_GUARD perms in Estate B")
        void differentPermsPerEstate() throws Exception {
            setSecurityContext(USER_ID, "ESTATE_ADMIN");
            TenantContext.setTenantId(TENANT_1);

            EstateMembership adminMembership = buildMembership(ESTATE_A, UserRole.ESTATE_ADMIN, TENANT_1);
            EstateMembership guardMembership = buildMembership(ESTATE_B, UserRole.SECURITY_GUARD, TENANT_1);

            when(membershipRepository.findActiveByUserIdAndEstateId(USER_ID, ESTATE_A))
                .thenReturn(Optional.of(adminMembership));
            when(membershipRepository.findActiveByUserIdAndEstateId(USER_ID, ESTATE_B))
                .thenReturn(Optional.of(guardMembership));

            when(permissionResolver.resolvePermissions(adminMembership))
                .thenReturn(Set.of("estate.read", "estate.update", "resident.create"));
            when(permissionResolver.resolvePermissions(guardMembership))
                .thenReturn(Set.of("gate.entry", "gate.exit"));

            // Request to Estate A
            MockHttpServletRequest reqA = new MockHttpServletRequest();
            reqA.addHeader("X-ESTATE-ID", ESTATE_A.toString());
            MockHttpServletResponse resA = new MockHttpServletResponse();

            String[] roleA = new String[1];
            Set<String> permsA = new HashSet<>();
            doAnswer(inv -> {
                roleA[0] = EstateContext.getRole();
                permsA.addAll(EstateContext.getPermissions());
                return null;
            }).when(filterChain).doFilter(reqA, resA);

            filter.doFilterInternal(reqA, resA, filterChain);

            assertThat(roleA[0]).isEqualTo("ESTATE_ADMIN");
            assertThat(permsA).contains("estate.update", "resident.create");
            assertThat(permsA).doesNotContain("gate.entry");

            // Request to Estate B
            MockHttpServletRequest reqB = new MockHttpServletRequest();
            reqB.addHeader("X-ESTATE-ID", ESTATE_B.toString());
            MockHttpServletResponse resB = new MockHttpServletResponse();

            String[] roleB = new String[1];
            Set<String> permsB = new HashSet<>();
            doAnswer(inv -> {
                roleB[0] = EstateContext.getRole();
                permsB.addAll(EstateContext.getPermissions());
                return null;
            }).when(filterChain).doFilter(reqB, resB);

            filter.doFilterInternal(reqB, resB, filterChain);

            assertThat(roleB[0]).isEqualTo("SECURITY_GUARD");
            assertThat(permsB).contains("gate.entry", "gate.exit");
            assertThat(permsB).doesNotContain("estate.update", "resident.create");
        }
    }

    // ═══════════════════════════════════════════════════════
    // 4. Missing X-ESTATE-ID header behavior
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("(4) Missing X-ESTATE-ID header behavior")
    class MissingEstateHeader {

        @Test
        @DisplayName("request without X-ESTATE-ID proceeds with empty EstateContext")
        void noHeaderProceeds() throws Exception {
            setSecurityContext(USER_ID, "ESTATE_ADMIN");

            MockHttpServletRequest request = new MockHttpServletRequest();
            // No X-ESTATE-ID header
            MockHttpServletResponse response = new MockHttpServletResponse();

            UUID[] capturedEstateId = new UUID[1];
            Set<String> capturedPerms = new HashSet<>();
            doAnswer(inv -> {
                capturedEstateId[0] = EstateContext.getEstateId();
                capturedPerms.addAll(EstateContext.getPermissions());
                return null;
            }).when(filterChain).doFilter(request, response);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
            verify(filterChain).doFilter(request, response);
            assertThat(capturedEstateId[0]).isNull();
            assertThat(capturedPerms).isEmpty();
        }

        @Test
        @DisplayName("blank X-ESTATE-ID header treated as missing")
        void blankHeaderTreatedAsMissing() throws Exception {
            setSecurityContext(USER_ID, "ESTATE_ADMIN");

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-ESTATE-ID", "   ");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("invalid UUID format returns 400 Bad Request")
        void invalidUuidFormat() throws Exception {
            setSecurityContext(USER_ID, "ESTATE_ADMIN");

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-ESTATE-ID", "not-a-uuid");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
            assertThat(response.getContentAsString()).contains("Invalid X-ESTATE-ID format");
            verify(filterChain, never()).doFilter(any(), any());
        }
    }

    // ═══════════════════════════════════════════════════════
    // 5. ThreadLocal cleanup after request
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("(5) ThreadLocal cleanup — EstateContext cleared after each request")
    class ThreadLocalCleanup {

        @Test
        @DisplayName("EstateContext is cleared after successful request")
        void clearedAfterSuccess() throws Exception {
            setSecurityContext(USER_ID, "ESTATE_ADMIN");
            TenantContext.setTenantId(TENANT_1);

            EstateMembership membership = buildMembership(ESTATE_A, UserRole.ESTATE_ADMIN, TENANT_1);
            when(membershipRepository.findActiveByUserIdAndEstateId(USER_ID, ESTATE_A))
                .thenReturn(Optional.of(membership));
            when(permissionResolver.resolvePermissions(membership))
                .thenReturn(Set.of("estate.read"));

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-ESTATE-ID", ESTATE_A.toString());
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, filterChain);

            // After filter completes, ThreadLocals must be cleared
            assertThat(EstateContext.getEstateId()).isNull();
            assertThat(EstateContext.getUserId()).isNull();
            assertThat(EstateContext.getRole()).isNull();
            assertThat(EstateContext.getPermissions()).isEmpty();
        }

        @Test
        @DisplayName("EstateContext is cleared even when filterChain throws exception")
        void clearedAfterException() throws Exception {
            setSecurityContext(USER_ID, "ESTATE_ADMIN");
            TenantContext.setTenantId(TENANT_1);

            EstateMembership membership = buildMembership(ESTATE_A, UserRole.ESTATE_ADMIN, TENANT_1);
            when(membershipRepository.findActiveByUserIdAndEstateId(USER_ID, ESTATE_A))
                .thenReturn(Optional.of(membership));
            when(permissionResolver.resolvePermissions(membership))
                .thenReturn(Set.of("estate.read"));

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-ESTATE-ID", ESTATE_A.toString());
            MockHttpServletResponse response = new MockHttpServletResponse();

            doThrow(new RuntimeException("Simulated error"))
                .when(filterChain).doFilter(request, response);

            try {
                filter.doFilterInternal(request, response, filterChain);
            } catch (RuntimeException e) {
                // expected
            }

            // ThreadLocals must STILL be cleared
            assertThat(EstateContext.getEstateId()).isNull();
            assertThat(EstateContext.getUserId()).isNull();
            assertThat(EstateContext.getRole()).isNull();
            assertThat(EstateContext.getPermissions()).isEmpty();
        }

        @Test
        @DisplayName("EstateContext is cleared after forbidden response (no membership)")
        void clearedAfterForbidden() throws Exception {
            setSecurityContext(USER_ID, "ESTATE_ADMIN");
            TenantContext.setTenantId(TENANT_1);

            when(membershipRepository.findActiveByUserIdAndEstateId(USER_ID, ESTATE_B))
                .thenReturn(Optional.empty());

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-ESTATE-ID", ESTATE_B.toString());
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, filterChain);

            assertThat(EstateContext.getEstateId()).isNull();
            assertThat(EstateContext.getPermissions()).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════
    // SUPER_ADMIN bypass
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("SUPER_ADMIN bypass")
    class SuperAdminBypass {

        @Test
        @DisplayName("SUPER_ADMIN gets all permissions without membership check")
        void superAdminBypass() throws Exception {
            setSecurityContext(USER_ID, "SUPER_ADMIN");
            TenantContext.setTenantId(TENANT_1);

            when(estateRepository.findByIdAndTenantId(ESTATE_A, TENANT_1))
                .thenReturn(Optional.of(new com.strataguard.core.entity.Estate()));
            when(permissionResolver.getDefaultPermissions(UserRole.SUPER_ADMIN))
                .thenReturn(List.of("estate.read", "estate.create", "estate.update", "estate.delete"));

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-ESTATE-ID", ESTATE_A.toString());
            MockHttpServletResponse response = new MockHttpServletResponse();

            Set<String> capturedPerms = new HashSet<>();
            String[] capturedRole = new String[1];
            doAnswer(inv -> {
                capturedRole[0] = EstateContext.getRole();
                capturedPerms.addAll(EstateContext.getPermissions());
                return null;
            }).when(filterChain).doFilter(request, response);

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
            assertThat(capturedRole[0]).isEqualTo("SUPER_ADMIN");
            assertThat(capturedPerms).contains("estate.read", "estate.create");
            // No membership lookup should happen for SUPER_ADMIN
            verify(membershipRepository, never()).findActiveByUserIdAndEstateId(any(), any());
        }
    }

    // ═══════════════════════════════════════════════════════
    // shouldNotFilter
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("shouldNotFilter — excluded paths")
    class ShouldNotFilter {

        @Test
        @DisplayName("auth endpoints are excluded from filter")
        void authExcluded() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/auth/login");
            assertThat(filter.shouldNotFilter(request)).isTrue();
        }

        @Test
        @DisplayName("actuator endpoints are excluded")
        void actuatorExcluded() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/actuator/health");
            assertThat(filter.shouldNotFilter(request)).isTrue();
        }

        @Test
        @DisplayName("normal API endpoints are NOT excluded")
        void normalApiNotExcluded() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/estates");
            assertThat(filter.shouldNotFilter(request)).isFalse();
        }
    }
}
