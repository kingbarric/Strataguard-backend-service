-- V6: Create notification tables

-- Notifications
CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL,
    recipient_id    UUID NOT NULL REFERENCES residents(id),
    channel         VARCHAR(50) NOT NULL,
    type            VARCHAR(50) NOT NULL,
    title           VARCHAR(500) NOT NULL,
    body            TEXT NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    metadata        JSONB,
    retry_count     INT NOT NULL DEFAULT 0,
    last_error      TEXT,
    sent_at         TIMESTAMPTZ,
    read_at         TIMESTAMPTZ,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);

CREATE INDEX idx_notifications_tenant_id ON notifications(tenant_id);
CREATE INDEX idx_notifications_recipient ON notifications(recipient_id, tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_notifications_status ON notifications(status, tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_notifications_recipient_status ON notifications(recipient_id, status, tenant_id) WHERE is_deleted = FALSE;

-- Notification Preferences
CREATE TABLE notification_preferences (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL,
    resident_id         UUID NOT NULL REFERENCES residents(id),
    channel             VARCHAR(50) NOT NULL,
    notification_type   VARCHAR(50) NOT NULL,
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255)
);

CREATE INDEX idx_notif_pref_tenant_id ON notification_preferences(tenant_id);
CREATE INDEX idx_notif_pref_resident ON notification_preferences(resident_id, tenant_id) WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX idx_notif_pref_unique ON notification_preferences(resident_id, channel, notification_type, tenant_id) WHERE is_deleted = FALSE;

-- Notification Templates
CREATE TABLE notification_templates (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL,
    name                VARCHAR(255) NOT NULL,
    notification_type   VARCHAR(50) NOT NULL,
    channel             VARCHAR(50),
    subject_template    VARCHAR(500),
    body_template       TEXT NOT NULL,
    estate_id           UUID REFERENCES estates(id),
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255)
);

CREATE INDEX idx_notif_template_tenant_id ON notification_templates(tenant_id);
CREATE INDEX idx_notif_template_type ON notification_templates(notification_type, tenant_id) WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX idx_notif_template_name_tenant ON notification_templates(name, tenant_id) WHERE is_deleted = FALSE;
