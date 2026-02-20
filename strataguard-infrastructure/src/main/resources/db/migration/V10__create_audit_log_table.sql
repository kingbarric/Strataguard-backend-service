-- =============================================
-- Phase 8: Audit Logging
-- =============================================

CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    actor_id        VARCHAR(255) NOT NULL,
    actor_name      VARCHAR(255),
    action          VARCHAR(30) NOT NULL,
    entity_type     VARCHAR(100) NOT NULL,
    entity_id       VARCHAR(255),
    old_value       TEXT,
    new_value       TEXT,
    ip_address      VARCHAR(50),
    user_agent      VARCHAR(500),
    description     VARCHAR(500),
    timestamp       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    previous_hash   VARCHAR(128),
    hash            VARCHAR(128) NOT NULL
);

-- Indexes for efficient querying
CREATE INDEX idx_audit_log_tenant_id ON audit_logs(tenant_id);
CREATE INDEX idx_audit_log_actor_id ON audit_logs(actor_id);
CREATE INDEX idx_audit_log_entity_type ON audit_logs(entity_type);
CREATE INDEX idx_audit_log_entity_id ON audit_logs(entity_id);
CREATE INDEX idx_audit_log_action ON audit_logs(action);
CREATE INDEX idx_audit_log_timestamp ON audit_logs(timestamp);

-- Composite index for common query patterns
CREATE INDEX idx_audit_log_tenant_timestamp ON audit_logs(tenant_id, timestamp DESC);
CREATE INDEX idx_audit_log_entity_lookup ON audit_logs(tenant_id, entity_type, entity_id);
