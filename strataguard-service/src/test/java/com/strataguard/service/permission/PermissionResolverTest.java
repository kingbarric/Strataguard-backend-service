package com.strataguard.service.permission;

import com.strataguard.core.entity.EstateMembership;
import com.strataguard.core.enums.MembershipStatus;
import com.strataguard.core.enums.UserRole;
import com.strataguard.infrastructure.repository.RolePermissionDefaultRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionResolver — RBAC permission resolution")
class PermissionResolverTest {

    @Mock
    private RolePermissionDefaultRepository rolePermissionDefaultRepository;

    @InjectMocks
    private PermissionResolver permissionResolver;

    private EstateMembership buildMembership(UserRole role) {
        EstateMembership m = new EstateMembership();
        m.setUserId("user-1");
        m.setEstateId(java.util.UUID.randomUUID());
        m.setRole(role);
        m.setStatus(MembershipStatus.ACTIVE);
        return m;
    }

    // ─────────────────────────────────────────────────────
    // 1. Basic permission resolution (formula correctness)
    // ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("resolvePermissions — formula: (defaults + granted) - revoked")
    class ResolvePermissions {

        @Test
        @DisplayName("returns default role permissions when no custom overrides")
        void defaultPermissionsOnly() {
            EstateMembership m = buildMembership(UserRole.SECURITY_GUARD);
            when(rolePermissionDefaultRepository.findPermissionsByRole(UserRole.SECURITY_GUARD))
                .thenReturn(List.of("gate.entry", "gate.exit", "visitor.checkin"));

            Set<String> result = permissionResolver.resolvePermissions(m);

            assertThat(result).containsExactlyInAnyOrder("gate.entry", "gate.exit", "visitor.checkin");
        }

        @Test
        @DisplayName("adds custom granted permissions on top of defaults")
        void customGrantedAdded() {
            EstateMembership m = buildMembership(UserRole.SECURITY_GUARD);
            m.setCustomPermissionsGranted(new String[]{"incident.create", "cctv.read"});

            when(rolePermissionDefaultRepository.findPermissionsByRole(UserRole.SECURITY_GUARD))
                .thenReturn(List.of("gate.entry", "gate.exit"));

            Set<String> result = permissionResolver.resolvePermissions(m);

            assertThat(result).containsExactlyInAnyOrder(
                "gate.entry", "gate.exit", "incident.create", "cctv.read");
        }

        @Test
        @DisplayName("removes custom revoked permissions from defaults")
        void customRevokedRemoved() {
            EstateMembership m = buildMembership(UserRole.ESTATE_ADMIN);
            m.setCustomPermissionsRevoked(new String[]{"estate.delete", "unit.delete"});

            when(rolePermissionDefaultRepository.findPermissionsByRole(UserRole.ESTATE_ADMIN))
                .thenReturn(List.of("estate.read", "estate.update", "estate.delete",
                                    "unit.read", "unit.create", "unit.delete"));

            Set<String> result = permissionResolver.resolvePermissions(m);

            assertThat(result)
                .contains("estate.read", "estate.update", "unit.read", "unit.create")
                .doesNotContain("estate.delete", "unit.delete");
        }

        @Test
        @DisplayName("grant + revoke applied together: grant first, then revoke wins")
        void grantAndRevokeTogether() {
            EstateMembership m = buildMembership(UserRole.FRONT_DESK);
            // Grant cctv.read, but also revoke it — revoke should win
            m.setCustomPermissionsGranted(new String[]{"cctv.read", "incident.create"});
            m.setCustomPermissionsRevoked(new String[]{"cctv.read"});

            when(rolePermissionDefaultRepository.findPermissionsByRole(UserRole.FRONT_DESK))
                .thenReturn(List.of("visitor.read", "visitor.create"));

            Set<String> result = permissionResolver.resolvePermissions(m);

            assertThat(result)
                .contains("visitor.read", "visitor.create", "incident.create")
                .doesNotContain("cctv.read");
        }

        @Test
        @DisplayName("revoking a non-existent permission is harmless")
        void revokeNonExistent() {
            EstateMembership m = buildMembership(UserRole.FRONT_DESK);
            m.setCustomPermissionsRevoked(new String[]{"nuclear.launch"});

            when(rolePermissionDefaultRepository.findPermissionsByRole(UserRole.FRONT_DESK))
                .thenReturn(List.of("visitor.read"));

            Set<String> result = permissionResolver.resolvePermissions(m);

            assertThat(result).containsExactly("visitor.read");
        }

        @Test
        @DisplayName("null custom arrays treated as empty")
        void nullCustomArrays() {
            EstateMembership m = buildMembership(UserRole.FRONT_DESK);
            m.setCustomPermissionsGranted(null);
            m.setCustomPermissionsRevoked(null);

            when(rolePermissionDefaultRepository.findPermissionsByRole(UserRole.FRONT_DESK))
                .thenReturn(List.of("visitor.read"));

            Set<String> result = permissionResolver.resolvePermissions(m);

            assertThat(result).containsExactly("visitor.read");
        }

        @Test
        @DisplayName("result set is unmodifiable")
        void resultIsUnmodifiable() {
            EstateMembership m = buildMembership(UserRole.FRONT_DESK);
            when(rolePermissionDefaultRepository.findPermissionsByRole(UserRole.FRONT_DESK))
                .thenReturn(List.of("visitor.read"));

            Set<String> result = permissionResolver.resolvePermissions(m);

            org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> result.add("hacked.permission"));
        }
    }

    // ─────────────────────────────────────────────────────
    // 2. Permission downgrade — immediate effect
    // ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Permission downgrade — immediate effect on next resolve")
    class PermissionDowngrade {

        @Test
        @DisplayName("removing a default permission is reflected on next resolvePermissions call")
        void downgradeReflectedImmediately() {
            EstateMembership m = buildMembership(UserRole.ESTATE_ADMIN);

            // First call: full permissions
            when(rolePermissionDefaultRepository.findPermissionsByRole(UserRole.ESTATE_ADMIN))
                .thenReturn(List.of("estate.read", "estate.update", "resident.create"));

            Set<String> before = permissionResolver.resolvePermissions(m);
            assertThat(before).contains("resident.create");

            // Simulate permission downgrade: resident.create removed from defaults
            when(rolePermissionDefaultRepository.findPermissionsByRole(UserRole.ESTATE_ADMIN))
                .thenReturn(List.of("estate.read", "estate.update"));

            Set<String> after = permissionResolver.resolvePermissions(m);
            assertThat(after).doesNotContain("resident.create");
        }

        @Test
        @DisplayName("revoking via custom override takes immediate effect")
        void customRevokeDowngrade() {
            EstateMembership m = buildMembership(UserRole.FINANCE_OFFICER);

            when(rolePermissionDefaultRepository.findPermissionsByRole(UserRole.FINANCE_OFFICER))
                .thenReturn(List.of("invoice.read", "invoice.create", "payment.read"));

            // Before revoke
            Set<String> before = permissionResolver.resolvePermissions(m);
            assertThat(before).contains("invoice.create");

            // Add custom revoke
            m.setCustomPermissionsRevoked(new String[]{"invoice.create"});

            Set<String> after = permissionResolver.resolvePermissions(m);
            assertThat(after)
                .contains("invoice.read", "payment.read")
                .doesNotContain("invoice.create");
        }
    }

    // ─────────────────────────────────────────────────────
    // 3. Multi-estate: same user, different roles
    // ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Multi-estate user — different roles per estate")
    class MultiEstateUser {

        @Test
        @DisplayName("same user gets different permissions in different estates")
        void differentRolesPerEstate() {
            // Estate A: user is ESTATE_ADMIN
            EstateMembership membershipA = buildMembership(UserRole.ESTATE_ADMIN);
            membershipA.setUserId("user-multi");

            // Estate B: user is SECURITY_GUARD
            EstateMembership membershipB = buildMembership(UserRole.SECURITY_GUARD);
            membershipB.setUserId("user-multi");

            when(rolePermissionDefaultRepository.findPermissionsByRole(UserRole.ESTATE_ADMIN))
                .thenReturn(List.of("estate.read", "estate.update", "resident.create", "unit.create"));
            when(rolePermissionDefaultRepository.findPermissionsByRole(UserRole.SECURITY_GUARD))
                .thenReturn(List.of("gate.entry", "gate.exit", "visitor.checkin"));

            Set<String> permsA = permissionResolver.resolvePermissions(membershipA);
            Set<String> permsB = permissionResolver.resolvePermissions(membershipB);

            // Estate A: admin-level
            assertThat(permsA).contains("estate.update", "resident.create", "unit.create");
            assertThat(permsA).doesNotContain("gate.entry");

            // Estate B: guard-level
            assertThat(permsB).contains("gate.entry", "gate.exit", "visitor.checkin");
            assertThat(permsB).doesNotContain("estate.update", "resident.create");
        }

        @Test
        @DisplayName("custom overrides are per-membership, not per-user")
        void customOverridesPerMembership() {
            EstateMembership membershipA = buildMembership(UserRole.SECURITY_GUARD);
            membershipA.setUserId("user-multi");
            membershipA.setCustomPermissionsGranted(new String[]{"incident.create"});

            EstateMembership membershipB = buildMembership(UserRole.SECURITY_GUARD);
            membershipB.setUserId("user-multi");
            // No custom grants on membership B

            when(rolePermissionDefaultRepository.findPermissionsByRole(UserRole.SECURITY_GUARD))
                .thenReturn(List.of("gate.entry", "gate.exit"));

            Set<String> permsA = permissionResolver.resolvePermissions(membershipA);
            Set<String> permsB = permissionResolver.resolvePermissions(membershipB);

            assertThat(permsA).contains("incident.create");
            assertThat(permsB).doesNotContain("incident.create");
        }
    }

    // ─────────────────────────────────────────────────────
    // 4. isSuperAdmin helper
    // ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("isSuperAdmin")
    class IsSuperAdmin {

        @Test
        @DisplayName("detects SUPER_ADMIN from JWT roles")
        void detectsSuperAdmin() {
            assertThat(permissionResolver.isSuperAdmin(List.of("SUPER_ADMIN"))).isTrue();
            assertThat(permissionResolver.isSuperAdmin(List.of("ROLE_SUPER_ADMIN"))).isTrue();
        }

        @Test
        @DisplayName("returns false for non-admin roles")
        void nonAdmin() {
            assertThat(permissionResolver.isSuperAdmin(List.of("ESTATE_ADMIN"))).isFalse();
            assertThat(permissionResolver.isSuperAdmin(List.of("SECURITY_GUARD"))).isFalse();
            assertThat(permissionResolver.isSuperAdmin(List.of())).isFalse();
        }
    }
}
