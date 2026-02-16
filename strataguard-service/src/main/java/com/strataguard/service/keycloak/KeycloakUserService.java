package com.strataguard.service.keycloak;

import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakUserService {

    private final Keycloak keycloakAdminClient;

    @Value("${keycloak.admin.realm}")
    private String realm;

    public String createUser(String email, String firstName, String lastName,
                             String password, String role, String tenantId) {
        UsersResource usersResource = getRealmResource().users();

        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(email);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmailVerified(true);
        user.setAttributes(Map.of("tenant_id", List.of(tenantId)));

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        user.setCredentials(List.of(credential));

        try (Response response = usersResource.create(user)) {
            if (response.getStatus() == 201) {
                String userId = extractUserId(response);
                if (role != null) {
                    assignRole(userId, role);
                }
                log.info("Created Keycloak user: {} with role: {} for tenant: {}", email, role, tenantId);
                return userId;
            } else {
                String body = response.readEntity(String.class);
                log.error("Failed to create user in Keycloak: status={}, body={}", response.getStatus(), body);
                throw new RuntimeException("Failed to create user in Keycloak: " + body);
            }
        }
    }

    public void assignRole(String userId, String roleName) {
        RealmResource realmResource = getRealmResource();
        RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
        realmResource.users().get(userId).roles().realmLevel()
                .add(Collections.singletonList(role));
        log.info("Assigned role {} to user {}", roleName, userId);
    }

    public void deleteUser(String userId) {
        getRealmResource().users().delete(userId);
        log.info("Deleted Keycloak user: {}", userId);
    }

    public List<UserRepresentation> searchUsers(String search, int first, int max) {
        return getRealmResource().users().search(search, first, max);
    }

    private RealmResource getRealmResource() {
        return keycloakAdminClient.realm(realm);
    }

    private String extractUserId(Response response) {
        String location = response.getHeaderString("Location");
        if (location != null) {
            return location.substring(location.lastIndexOf('/') + 1);
        }
        throw new RuntimeException("Could not extract user ID from Keycloak response");
    }
}
