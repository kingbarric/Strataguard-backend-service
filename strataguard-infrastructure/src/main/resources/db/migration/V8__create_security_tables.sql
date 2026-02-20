-- V8: Create security module tables (staff, patrols, emergencies, cameras, incidents)

-- Staff (general-purpose estate workers: security, maintenance, cleaners, etc.)
CREATE TABLE staff (
    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id                   UUID NOT NULL,
    user_id                     VARCHAR(255),
    estate_id                   UUID NOT NULL REFERENCES estates(id),
    first_name                  VARCHAR(255) NOT NULL,
    last_name                   VARCHAR(255) NOT NULL,
    phone                       VARCHAR(50),
    email                       VARCHAR(255),
    department                  VARCHAR(50) NOT NULL,
    position                    VARCHAR(255),
    badge_number                VARCHAR(100),
    status                      VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    photo_url                   VARCHAR(1000),
    hire_date                   DATE,
    emergency_contact_name      VARCHAR(255),
    emergency_contact_phone     VARCHAR(50),
    notes                       TEXT,
    is_active                   BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted                  BOOLEAN NOT NULL DEFAULT FALSE,
    version                     BIGINT NOT NULL DEFAULT 0,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ,
    created_by                  VARCHAR(255),
    updated_by                  VARCHAR(255)
);

CREATE INDEX idx_staff_tenant_id ON staff(tenant_id);
CREATE INDEX idx_staff_estate_id ON staff(estate_id, tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_staff_department ON staff(department, tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_staff_user_id ON staff(user_id, tenant_id) WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX idx_staff_badge_number_tenant ON staff(badge_number, tenant_id) WHERE is_deleted = FALSE AND badge_number IS NOT NULL;

-- Staff Shifts
CREATE TABLE staff_shifts (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL,
    staff_id            UUID NOT NULL REFERENCES staff(id),
    estate_id           UUID NOT NULL REFERENCES estates(id),
    shift_type          VARCHAR(50) NOT NULL,
    start_time          TIME NOT NULL,
    end_time            TIME NOT NULL,
    days_of_week        VARCHAR(100),
    effective_from      DATE NOT NULL,
    effective_to        DATE,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255)
);

CREATE INDEX idx_staff_shifts_tenant_id ON staff_shifts(tenant_id);
CREATE INDEX idx_staff_shifts_staff_id ON staff_shifts(staff_id, tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_staff_shifts_estate_id ON staff_shifts(estate_id, tenant_id) WHERE is_deleted = FALSE;

-- Patrol Checkpoints
CREATE TABLE patrol_checkpoints (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL,
    estate_id       UUID NOT NULL REFERENCES estates(id),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    qr_code         VARCHAR(255) NOT NULL,
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    sort_order      INT NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);

CREATE INDEX idx_patrol_checkpoints_tenant_id ON patrol_checkpoints(tenant_id);
CREATE INDEX idx_patrol_checkpoints_estate_id ON patrol_checkpoints(estate_id, tenant_id) WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX idx_patrol_checkpoints_qr_code_tenant ON patrol_checkpoints(qr_code, tenant_id) WHERE is_deleted = FALSE;

-- Patrol Sessions
CREATE TABLE patrol_sessions (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id               UUID NOT NULL,
    staff_id                UUID NOT NULL REFERENCES staff(id),
    estate_id               UUID NOT NULL REFERENCES estates(id),
    started_at              TIMESTAMPTZ NOT NULL,
    completed_at            TIMESTAMPTZ,
    status                  VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
    total_checkpoints       INT NOT NULL DEFAULT 0,
    scanned_checkpoints     INT NOT NULL DEFAULT 0,
    completion_percentage   DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    notes                   TEXT,
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    version                 BIGINT NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ,
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255)
);

CREATE INDEX idx_patrol_sessions_tenant_id ON patrol_sessions(tenant_id);
CREATE INDEX idx_patrol_sessions_staff_id ON patrol_sessions(staff_id, tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_patrol_sessions_estate_id ON patrol_sessions(estate_id, tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_patrol_sessions_status ON patrol_sessions(status, tenant_id) WHERE is_deleted = FALSE;

-- Patrol Scans
CREATE TABLE patrol_scans (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL,
    session_id      UUID NOT NULL REFERENCES patrol_sessions(id),
    checkpoint_id   UUID NOT NULL REFERENCES patrol_checkpoints(id),
    scanned_at      TIMESTAMPTZ NOT NULL,
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    notes           TEXT,
    photo_url       VARCHAR(1000),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);

CREATE INDEX idx_patrol_scans_tenant_id ON patrol_scans(tenant_id);
CREATE INDEX idx_patrol_scans_session_id ON patrol_scans(session_id, tenant_id) WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX uk_patrol_scan_session_checkpoint ON patrol_scans(session_id, checkpoint_id) WHERE is_deleted = FALSE;

-- Emergency Alerts
CREATE TABLE emergency_alerts (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id               UUID NOT NULL,
    resident_id             UUID NOT NULL REFERENCES residents(id),
    estate_id               UUID NOT NULL REFERENCES estates(id),
    unit_id                 UUID REFERENCES units(id),
    alert_type              VARCHAR(50) NOT NULL,
    status                  VARCHAR(50) NOT NULL DEFAULT 'TRIGGERED',
    description             TEXT,
    latitude                DOUBLE PRECISION,
    longitude               DOUBLE PRECISION,
    acknowledged_by         UUID,
    acknowledged_at         TIMESTAMPTZ,
    responded_by            UUID,
    responded_at            TIMESTAMPTZ,
    resolved_by             UUID,
    resolved_at             TIMESTAMPTZ,
    resolution_notes        TEXT,
    response_time_seconds   BIGINT,
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    version                 BIGINT NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ,
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255)
);

CREATE INDEX idx_emergency_alerts_tenant_id ON emergency_alerts(tenant_id);
CREATE INDEX idx_emergency_alerts_estate_id ON emergency_alerts(estate_id, tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_emergency_alerts_status ON emergency_alerts(status, tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_emergency_alerts_resident_id ON emergency_alerts(resident_id, tenant_id) WHERE is_deleted = FALSE;

-- CCTV Cameras
CREATE TABLE cctv_cameras (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL,
    estate_id           UUID NOT NULL REFERENCES estates(id),
    camera_name         VARCHAR(255) NOT NULL,
    camera_code         VARCHAR(100) NOT NULL,
    camera_type         VARCHAR(50) NOT NULL,
    zone                VARCHAR(50) NOT NULL,
    location            VARCHAR(500),
    stream_url          VARCHAR(1000),
    ip_address          VARCHAR(50),
    status              VARCHAR(50) NOT NULL DEFAULT 'ONLINE',
    last_online_at      TIMESTAMPTZ,
    latitude            DOUBLE PRECISION,
    longitude           DOUBLE PRECISION,
    install_date        DATE,
    notes               TEXT,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255)
);

CREATE INDEX idx_cctv_cameras_tenant_id ON cctv_cameras(tenant_id);
CREATE INDEX idx_cctv_cameras_estate_id ON cctv_cameras(estate_id, tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_cctv_cameras_zone ON cctv_cameras(zone, estate_id, tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_cctv_cameras_status ON cctv_cameras(status, tenant_id) WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX idx_cctv_cameras_code_tenant ON cctv_cameras(camera_code, tenant_id) WHERE is_deleted = FALSE;

-- Camera Status Logs
CREATE TABLE camera_status_logs (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL,
    camera_id           UUID NOT NULL REFERENCES cctv_cameras(id),
    previous_status     VARCHAR(50),
    new_status          VARCHAR(50) NOT NULL,
    changed_at          TIMESTAMPTZ NOT NULL,
    reason              VARCHAR(500),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255)
);

CREATE INDEX idx_camera_status_logs_tenant_id ON camera_status_logs(tenant_id);
CREATE INDEX idx_camera_status_logs_camera_id ON camera_status_logs(camera_id, tenant_id) WHERE is_deleted = FALSE;

-- Security Incidents
CREATE TABLE security_incidents (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL,
    estate_id           UUID NOT NULL REFERENCES estates(id),
    incident_number     VARCHAR(50) NOT NULL,
    reported_by         UUID NOT NULL,
    reporter_type       VARCHAR(50) NOT NULL,
    title               VARCHAR(500) NOT NULL,
    description         TEXT,
    category            VARCHAR(50) NOT NULL,
    severity            VARCHAR(50) NOT NULL,
    status              VARCHAR(50) NOT NULL DEFAULT 'REPORTED',
    location            VARCHAR(500),
    latitude            DOUBLE PRECISION,
    longitude           DOUBLE PRECISION,
    photo_urls          JSONB,
    witnesses           JSONB,
    linked_alert_id     UUID REFERENCES emergency_alerts(id),
    assigned_to         UUID,
    assigned_at         TIMESTAMPTZ,
    resolved_at         TIMESTAMPTZ,
    resolved_by         UUID,
    resolution_notes    TEXT,
    closed_at           TIMESTAMPTZ,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255)
);

CREATE INDEX idx_security_incidents_tenant_id ON security_incidents(tenant_id);
CREATE INDEX idx_security_incidents_estate_id ON security_incidents(estate_id, tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_security_incidents_status ON security_incidents(status, tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_security_incidents_severity ON security_incidents(severity, tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_security_incidents_category ON security_incidents(category, tenant_id) WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX idx_security_incidents_number_tenant ON security_incidents(incident_number, tenant_id) WHERE is_deleted = FALSE;
