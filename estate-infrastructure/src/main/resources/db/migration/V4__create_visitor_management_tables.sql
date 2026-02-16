-- Phase 3.5: Visitor Management & QR Passes

-- Extend gate_access_logs: make sessionId/vehicleId/residentId nullable, add visitorId
ALTER TABLE gate_access_logs ALTER COLUMN session_id DROP NOT NULL;
ALTER TABLE gate_access_logs ALTER COLUMN vehicle_id DROP NOT NULL;
ALTER TABLE gate_access_logs ALTER COLUMN resident_id DROP NOT NULL;

ALTER TABLE gate_access_logs ADD COLUMN visitor_id UUID;

CREATE INDEX idx_gate_access_logs_visitor_id ON gate_access_logs(visitor_id);

-- Visitors table
CREATE TABLE visitors (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id               UUID NOT NULL,
    name                    VARCHAR(255) NOT NULL,
    phone                   VARCHAR(50),
    email                   VARCHAR(255),
    purpose                 TEXT,
    invited_by              UUID NOT NULL REFERENCES residents(id),
    visitor_type            VARCHAR(30) NOT NULL,
    vehicle_plate_number    VARCHAR(20),
    status                  VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ,
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255),
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_visitors_tenant_id ON visitors(tenant_id);
CREATE INDEX idx_visitors_invited_by ON visitors(invited_by);
CREATE INDEX idx_visitors_status ON visitors(status);
CREATE INDEX idx_visitors_phone ON visitors(phone);
CREATE INDEX idx_visitors_tenant_status ON visitors(tenant_id, status) WHERE is_deleted = FALSE;

-- Visit passes table
CREATE TABLE visit_passes (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id               UUID NOT NULL,
    visitor_id              UUID NOT NULL REFERENCES visitors(id),
    pass_code               VARCHAR(100) NOT NULL,
    qr_data                 TEXT,
    token                   TEXT NOT NULL,
    pass_type               VARCHAR(30) NOT NULL,
    valid_from              TIMESTAMPTZ NOT NULL,
    valid_to                TIMESTAMPTZ NOT NULL,
    max_entries             INTEGER,
    used_entries            INTEGER NOT NULL DEFAULT 0,
    status                  VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    verification_code       VARCHAR(6) NOT NULL,
    recurring_days          VARCHAR(100),
    recurring_start_time    TIME,
    recurring_end_time      TIME,
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ,
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255),
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_visit_passes_tenant_id ON visit_passes(tenant_id);
CREATE INDEX idx_visit_passes_visitor_id ON visit_passes(visitor_id);
CREATE INDEX idx_visit_passes_pass_code ON visit_passes(pass_code);
CREATE INDEX idx_visit_passes_verification_code ON visit_passes(verification_code);
CREATE UNIQUE INDEX idx_visit_passes_pass_code_unique ON visit_passes(pass_code) WHERE is_deleted = FALSE;

-- Blacklist table
CREATE TABLE blacklist (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id               UUID NOT NULL,
    name                    VARCHAR(255),
    phone                   VARCHAR(50),
    plate_number            VARCHAR(20),
    reason                  TEXT NOT NULL,
    active                  BOOLEAN NOT NULL DEFAULT TRUE,
    added_by                VARCHAR(255) NOT NULL,
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ,
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255),
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_blacklist_tenant_id ON blacklist(tenant_id);
CREATE INDEX idx_blacklist_plate_number ON blacklist(plate_number);
CREATE INDEX idx_blacklist_phone ON blacklist(phone);
CREATE INDEX idx_blacklist_tenant_active ON blacklist(tenant_id, active) WHERE is_deleted = FALSE;

-- Add foreign key from gate_access_logs to visitors
ALTER TABLE gate_access_logs ADD CONSTRAINT fk_gate_access_logs_visitor
    FOREIGN KEY (visitor_id) REFERENCES visitors(id);
