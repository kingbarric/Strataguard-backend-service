package com.strataguard.service.keycloak;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakUserServiceTest {

    @Mock
    private Keycloak keycloakAdminClient;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @InjectMocks
    private KeycloakUserService keycloakUserService;

    private static final String REALM = "test-realm";
    private static final String USER_ID = "abc-123-def-456";
    private static final String LOCATION_HEADER = "http://localhost:8080/admin/realms/test-realm/users/" + USER_ID;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(keycloakUserService, "realm", REALM);
        when(keycloakAdminClient.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
    }

    // ========================================================================
    // createUser
    // ========================================================================

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @DisplayName("should create user successfully and return userId when Keycloak returns 201")
        void shouldCreateUserSuccessfully() {
            Response response = mock(Response.class);
            when(response.getStatus()).thenReturn(201);
            when(response.getHeaderString("Location")).thenReturn(LOCATION_HEADER);
            when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);

            // Mock the role assignment chain
            RolesResource rolesResource = mock(RolesResource.class);
            RoleResource roleResource = mock(RoleResource.class);
            RoleRepresentation roleRepresentation = new RoleRepresentation();
            roleRepresentation.setName("ESTATE_ADMIN");
            UserResource userResource = mock(UserResource.class);
            RoleMappingResource roleMappingResource = mock(RoleMappingResource.class);
            RoleScopeResource roleScopeResource = mock(RoleScopeResource.class);

            when(realmResource.roles()).thenReturn(rolesResource);
            when(rolesResource.get("ESTATE_ADMIN")).thenReturn(roleResource);
            when(roleResource.toRepresentation()).thenReturn(roleRepresentation);
            when(usersResource.get(USER_ID)).thenReturn(userResource);
            when(userResource.roles()).thenReturn(roleMappingResource);
            when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);

            String result = keycloakUserService.createUser(
                    "john@example.com", "John", "Doe",
                    "password123", "ESTATE_ADMIN", "tenant-001"
            );

            assertThat(result).isEqualTo(USER_ID);

            verify(usersResource).create(any(UserRepresentation.class));
            verify(roleScopeResource).add(Collections.singletonList(roleRepresentation));
        }

        @Test
        @DisplayName("should extract userId from Location header correctly")
        void shouldExtractUserIdFromLocationHeader() {
            String customUserId = "custom-user-id-789";
            String locationHeader = "http://localhost:8080/admin/realms/test-realm/users/" + customUserId;

            Response response = mock(Response.class);
            when(response.getStatus()).thenReturn(201);
            when(response.getHeaderString("Location")).thenReturn(locationHeader);
            when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);

            // Mock the role assignment chain
            RolesResource rolesResource = mock(RolesResource.class);
            RoleResource roleResource = mock(RoleResource.class);
            RoleRepresentation roleRepresentation = new RoleRepresentation();
            UserResource userResource = mock(UserResource.class);
            RoleMappingResource roleMappingResource = mock(RoleMappingResource.class);
            RoleScopeResource roleScopeResource = mock(RoleScopeResource.class);

            when(realmResource.roles()).thenReturn(rolesResource);
            when(rolesResource.get("RESIDENT")).thenReturn(roleResource);
            when(roleResource.toRepresentation()).thenReturn(roleRepresentation);
            when(usersResource.get(customUserId)).thenReturn(userResource);
            when(userResource.roles()).thenReturn(roleMappingResource);
            when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);

            String result = keycloakUserService.createUser(
                    "jane@example.com", "Jane", "Smith",
                    "password456", "RESIDENT", "tenant-002"
            );

            assertThat(result).isEqualTo(customUserId);
        }

        @Test
        @DisplayName("should skip role assignment when role is null")
        void shouldSkipRoleAssignmentWhenRoleIsNull() {
            Response response = mock(Response.class);
            when(response.getStatus()).thenReturn(201);
            when(response.getHeaderString("Location")).thenReturn(LOCATION_HEADER);
            when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);

            String result = keycloakUserService.createUser(
                    "noel@example.com", "Noel", "Brown",
                    "password789", null, "tenant-003"
            );

            assertThat(result).isEqualTo(USER_ID);

            verify(usersResource).create(any(UserRepresentation.class));
            // roles() should never be called when role is null
        }

        @Test
        @DisplayName("should throw RuntimeException when Keycloak returns non-201 status")
        void shouldThrowRuntimeExceptionWhenNon201Status() {
            Response response = mock(Response.class);
            when(response.getStatus()).thenReturn(409);
            when(response.readEntity(String.class)).thenReturn("User already exists");
            when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);

            assertThatThrownBy(() -> keycloakUserService.createUser(
                    "john@example.com", "John", "Doe",
                    "password123", "ESTATE_ADMIN", "tenant-001"
            ))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to create user in Keycloak")
                    .hasMessageContaining("User already exists");
        }

        @Test
        @DisplayName("should throw RuntimeException when Location header is missing from 201 response")
        void shouldThrowRuntimeExceptionWhenLocationHeaderMissing() {
            Response response = mock(Response.class);
            when(response.getStatus()).thenReturn(201);
            when(response.getHeaderString("Location")).thenReturn(null);
            when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);

            assertThatThrownBy(() -> keycloakUserService.createUser(
                    "john@example.com", "John", "Doe",
                    "password123", "ESTATE_ADMIN", "tenant-001"
            ))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Could not extract user ID");
        }
    }

    // ========================================================================
    // assignRole
    // ========================================================================

    @Nested
    @DisplayName("assignRole")
    class AssignRole {

        @Test
        @DisplayName("should assign role to user successfully")
        void shouldAssignRoleSuccessfully() {
            RolesResource rolesResource = mock(RolesResource.class);
            RoleResource roleResource = mock(RoleResource.class);
            RoleRepresentation roleRepresentation = new RoleRepresentation();
            roleRepresentation.setName("FACILITY_MANAGER");
            UserResource userResource = mock(UserResource.class);
            RoleMappingResource roleMappingResource = mock(RoleMappingResource.class);
            RoleScopeResource roleScopeResource = mock(RoleScopeResource.class);

            when(realmResource.roles()).thenReturn(rolesResource);
            when(rolesResource.get("FACILITY_MANAGER")).thenReturn(roleResource);
            when(roleResource.toRepresentation()).thenReturn(roleRepresentation);
            when(usersResource.get(USER_ID)).thenReturn(userResource);
            when(userResource.roles()).thenReturn(roleMappingResource);
            when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);

            keycloakUserService.assignRole(USER_ID, "FACILITY_MANAGER");

            verify(rolesResource).get("FACILITY_MANAGER");
            verify(roleResource).toRepresentation();
            verify(roleScopeResource).add(Collections.singletonList(roleRepresentation));
        }
    }

    // ========================================================================
    // deleteUser
    // ========================================================================

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        @Test
        @DisplayName("should delete user via Keycloak admin client")
        void shouldDeleteUser() {
            keycloakUserService.deleteUser(USER_ID);

            verify(keycloakAdminClient).realm(REALM);
            verify(realmResource).users();
            verify(usersResource).delete(USER_ID);
        }

        @Test
        @DisplayName("should pass correct userId to Keycloak delete")
        void shouldPassCorrectUserIdToDelete() {
            String specificUserId = "specific-user-to-delete";

            keycloakUserService.deleteUser(specificUserId);

            verify(usersResource).delete(specificUserId);
        }
    }

    // ========================================================================
    // searchUsers
    // ========================================================================

    @Nested
    @DisplayName("searchUsers")
    class SearchUsers {

        @Test
        @DisplayName("should delegate search to Keycloak users resource")
        void shouldDelegateSearchToKeycloak() {
            UserRepresentation user1 = new UserRepresentation();
            user1.setUsername("john@example.com");
            user1.setFirstName("John");

            UserRepresentation user2 = new UserRepresentation();
            user2.setUsername("jane@example.com");
            user2.setFirstName("Jane");

            when(usersResource.search("john", 0, 10)).thenReturn(List.of(user1, user2));

            List<UserRepresentation> results = keycloakUserService.searchUsers("john", 0, 10);

            assertThat(results).hasSize(2);
            assertThat(results.get(0).getUsername()).isEqualTo("john@example.com");
            assertThat(results.get(1).getUsername()).isEqualTo("jane@example.com");

            verify(usersResource).search("john", 0, 10);
        }

        @Test
        @DisplayName("should return empty list when no users match search")
        void shouldReturnEmptyListWhenNoMatch() {
            when(usersResource.search("nonexistent", 0, 10)).thenReturn(List.of());

            List<UserRepresentation> results = keycloakUserService.searchUsers("nonexistent", 0, 10);

            assertThat(results).isEmpty();

            verify(usersResource).search("nonexistent", 0, 10);
        }

        @Test
        @DisplayName("should pass correct pagination parameters to Keycloak")
        void shouldPassCorrectPaginationParameters() {
            when(usersResource.search("test", 20, 5)).thenReturn(List.of());

            keycloakUserService.searchUsers("test", 20, 5);

            verify(usersResource).search("test", 20, 5);
        }
    }
}
