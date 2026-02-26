package com.strataguard.core.enums;

public enum UserRole {
    // Platform scope
    SUPER_ADMIN,

    // Portfolio scope
    PORTFOLIO_ADMIN,
    PORTFOLIO_VIEWER,

    // Estate scope - Staff
    ESTATE_ADMIN,
    FINANCE_OFFICER,
    SECURITY_MANAGER,
    SECURITY_GUARD,
    FACILITY_MANAGER,
    FRONT_DESK,

    // Estate scope - Residents
    RESIDENT_PRIMARY,
    RESIDENT_DEPENDENT,

    // DEPRECATED - kept for backward compatibility during migration
    @Deprecated
    RESIDENT
}
