-- V3: Add QR sticker code to vehicles and create gate control tables

-- Add qr_sticker_code column to vehicles
ALTER TABLE vehicles ADD COLUMN qr_sticker_code VARCHAR(50);

-- Backfill existing vehicles with generated QR sticker codes
UPDATE vehicles SET qr_sticker_code = 'VEH-' || uuid_generate_v4() WHERE qr_sticker_code IS NULL;

-- Make it NOT NULL after backfill
ALTER TABLE vehicles ALTER COLUMN qr_sticker_code SET NOT NULL;

-- Create unique index on qr_sticker_code per tenant
CREATE UNIQUE INDEX idx_vehicles_qr_sticker_code_tenant_unique ON vehicles(qr_sticker_code, tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_vehicles_qr_sticker_code ON vehicles(qr_sticker_code);

-- Gate Sessions: tracks vehicle entry/exit through the gate
CREATE TABLE gate_sessions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL,
    vehicle_id      UUID NOT NULL REFERENCES vehicles(id),
    resident_id     UUID NOT NULL REFERENCES residents(id),
    plate_number    VARCHAR(20) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    entry_time      TIMESTAMPTZ NOT NULL,
    exit_time       TIMESTAMPTZ,
    entry_guard_id  VARCHAR(255) NOT NULL,
    exit_guard_id   VARCHAR(255),
    entry_note      TEXT,
    exit_note       TEXT,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_gate_sessions_tenant_id ON gate_sessions(tenant_id);
CREATE INDEX idx_gate_sessions_vehicle_id ON gate_sessions(vehicle_id);
CREATE INDEX idx_gate_sessions_resident_id ON gate_sessions(resident_id);
CREATE INDEX idx_gate_sessions_status ON gate_sessions(status);

-- Partial unique index: only one OPEN session per vehicle per tenant
CREATE UNIQUE INDEX idx_gate_sessions_vehicle_open_unique ON gate_sessions(vehicle_id, tenant_id)
    WHERE status = 'OPEN' AND is_deleted = FALSE;

-- Gate Access Logs: audit trail for all gate events
CREATE TABLE gate_access_logs (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL,
    session_id      UUID NOT NULL REFERENCES gate_sessions(id),
    vehicle_id      UUID NOT NULL REFERENCES vehicles(id),
    resident_id     UUID NOT NULL REFERENCES residents(id),
    event_type      VARCHAR(50) NOT NULL,
    guard_id        VARCHAR(255) NOT NULL,
    details         TEXT,
    success         BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_gate_access_logs_tenant_id ON gate_access_logs(tenant_id);
CREATE INDEX idx_gate_access_logs_session_id ON gate_access_logs(session_id);
CREATE INDEX idx_gate_access_logs_vehicle_id ON gate_access_logs(vehicle_id);

-- Exit Approval Requests: remote approval for exit without phone
CREATE TABLE exit_approval_requests (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL,
    session_id      UUID NOT NULL REFERENCES gate_sessions(id),
    vehicle_id      UUID NOT NULL REFERENCES vehicles(id),
    resident_id     UUID NOT NULL REFERENCES residents(id),
    guard_id        VARCHAR(255) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    expires_at      TIMESTAMPTZ NOT NULL,
    responded_at    TIMESTAMPTZ,
    note            TEXT,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_exit_approval_requests_tenant_id ON exit_approval_requests(tenant_id);
CREATE INDEX idx_exit_approval_requests_session_id ON exit_approval_requests(session_id);
CREATE INDEX idx_exit_approval_requests_resident_id ON exit_approval_requests(resident_id);
CREATE INDEX idx_exit_approval_requests_status ON exit_approval_requests(status);
