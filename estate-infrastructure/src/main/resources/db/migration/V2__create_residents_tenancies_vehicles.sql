-- V2: Create residents, tenancies, and vehicles tables

CREATE TABLE residents (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id               UUID NOT NULL,
    user_id                 VARCHAR(255),
    first_name              VARCHAR(255) NOT NULL,
    last_name               VARCHAR(255) NOT NULL,
    phone                   VARCHAR(50),
    email                   VARCHAR(255),
    emergency_contact_name  VARCHAR(255),
    emergency_contact_phone VARCHAR(50),
    profile_photo_url       VARCHAR(500),
    status                  VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    active                  BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ,
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255),
    version                 BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_residents_tenant_id ON residents(tenant_id);
CREATE INDEX idx_residents_user_id ON residents(user_id);
CREATE INDEX idx_residents_email ON residents(email);
CREATE UNIQUE INDEX idx_residents_user_id_tenant_unique ON residents(user_id, tenant_id) WHERE user_id IS NOT NULL AND is_deleted = FALSE;
CREATE UNIQUE INDEX idx_residents_email_tenant_unique ON residents(email, tenant_id) WHERE email IS NOT NULL AND is_deleted = FALSE;

CREATE TABLE tenancies (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL,
    resident_id     UUID NOT NULL REFERENCES residents(id),
    unit_id         UUID NOT NULL REFERENCES units(id),
    tenancy_type    VARCHAR(30) NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE,
    status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    lease_reference VARCHAR(255),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_tenancies_tenant_id ON tenancies(tenant_id);
CREATE INDEX idx_tenancies_resident_id ON tenancies(resident_id);
CREATE INDEX idx_tenancies_unit_id ON tenancies(unit_id);
CREATE INDEX idx_tenancies_status ON tenancies(tenant_id, status) WHERE is_deleted = FALSE;

CREATE TABLE vehicles (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL,
    resident_id     UUID NOT NULL REFERENCES residents(id),
    plate_number    VARCHAR(20) NOT NULL,
    make            VARCHAR(100),
    model           VARCHAR(100),
    color           VARCHAR(50),
    vehicle_type    VARCHAR(30) NOT NULL,
    sticker_number  VARCHAR(50),
    status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    photo_url       VARCHAR(500),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_vehicles_tenant_id ON vehicles(tenant_id);
CREATE INDEX idx_vehicles_resident_id ON vehicles(resident_id);
CREATE INDEX idx_vehicles_plate_number ON vehicles(plate_number);
CREATE UNIQUE INDEX idx_vehicles_plate_tenant_unique ON vehicles(plate_number, tenant_id) WHERE is_deleted = FALSE;
