package com.strataguard.core.config;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public final class EstateContext {

    private static final ThreadLocal<UUID> CURRENT_ESTATE = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_ROLE = new ThreadLocal<>();
    private static final ThreadLocal<Set<String>> CURRENT_PERMISSIONS = new ThreadLocal<>();

    private EstateContext() {}

    public static void setEstateId(UUID estateId) { CURRENT_ESTATE.set(estateId); }
    public static UUID getEstateId() { return CURRENT_ESTATE.get(); }

    public static void setUserId(String userId) { CURRENT_USER_ID.set(userId); }
    public static String getUserId() { return CURRENT_USER_ID.get(); }

    public static void setRole(String role) { CURRENT_ROLE.set(role); }
    public static String getRole() { return CURRENT_ROLE.get(); }

    public static void setPermissions(Set<String> permissions) {
        CURRENT_PERMISSIONS.set(Collections.unmodifiableSet(permissions));
    }

    public static Set<String> getPermissions() {
        Set<String> perms = CURRENT_PERMISSIONS.get();
        return perms != null ? perms : Collections.emptySet();
    }

    public static boolean hasPermission(String permission) {
        return getPermissions().contains(permission);
    }

    public static UUID requireEstateId() {
        UUID estateId = CURRENT_ESTATE.get();
        if (estateId == null) {
            throw new IllegalStateException("Estate context not set. Ensure X-ESTATE-ID header is provided.");
        }
        return estateId;
    }

    public static void clear() {
        CURRENT_ESTATE.remove();
        CURRENT_USER_ID.remove();
        CURRENT_ROLE.remove();
        CURRENT_PERMISSIONS.remove();
    }
}
