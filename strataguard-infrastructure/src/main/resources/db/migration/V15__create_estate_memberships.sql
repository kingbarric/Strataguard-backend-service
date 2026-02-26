-- V15: Create estate memberships and role permission defaults tables for RBAC overhaul

-- Estate Memberships: links a Keycloak user to an estate with a role
CREATE TABLE estate_memberships (
    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id                   UUID NOT NULL,
    user_id                     VARCHAR(255) NOT NULL,
    estate_id                   UUID NOT NULL REFERENCES estates(id),
    role                        VARCHAR(50) NOT NULL,
    status                      VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    display_name                VARCHAR(500),
    custom_permissions          TEXT[],
    custom_permissions_revoked  TEXT[],
    is_deleted                  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ,
    created_by                  VARCHAR(255),
    updated_by                  VARCHAR(255),
    version                     BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_em_tenant_id ON estate_memberships(tenant_id);
CREATE INDEX idx_em_user_id ON estate_memberships(user_id);
CREATE INDEX idx_em_estate_id ON estate_memberships(estate_id);
CREATE INDEX idx_em_user_estate ON estate_memberships(user_id, estate_id);
CREATE INDEX idx_em_status ON estate_memberships(status) WHERE is_deleted = FALSE;

-- Unique constraint: one role per user per estate
CREATE UNIQUE INDEX idx_em_user_estate_unique
    ON estate_memberships(user_id, estate_id, tenant_id) WHERE is_deleted = FALSE;

-- Role Permission Defaults: seed table for default permissions per role
CREATE TABLE role_permission_defaults (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    role        VARCHAR(50) NOT NULL,
    permission  VARCHAR(100) NOT NULL,
    UNIQUE(role, permission)
);

CREATE INDEX idx_rpd_role ON role_permission_defaults(role);
