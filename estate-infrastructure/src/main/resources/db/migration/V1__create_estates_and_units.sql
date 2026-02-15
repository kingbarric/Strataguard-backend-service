-- V1: Create estates and units tables

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE estates (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL,
    name            VARCHAR(255) NOT NULL,
    address         VARCHAR(500) NOT NULL,
    city            VARCHAR(100),
    state           VARCHAR(100),
    country         VARCHAR(100),
    estate_type     VARCHAR(50) NOT NULL,
    description     TEXT,
    logo_url        VARCHAR(500),
    contact_email   VARCHAR(255),
    contact_phone   VARCHAR(50),
    total_units     INTEGER,
    settings        JSONB,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_estates_tenant_id ON estates(tenant_id);
CREATE INDEX idx_estates_name ON estates(name);
CREATE INDEX idx_estates_tenant_active ON estates(tenant_id, active) WHERE is_deleted = FALSE;

CREATE TABLE units (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL,
    estate_id       UUID NOT NULL REFERENCES estates(id),
    unit_number     VARCHAR(50) NOT NULL,
    block_or_zone   VARCHAR(100),
    unit_type       VARCHAR(50) NOT NULL,
    floor           INTEGER,
    status          VARCHAR(30) NOT NULL DEFAULT 'VACANT',
    bedrooms        INTEGER,
    bathrooms       INTEGER,
    square_meters   DOUBLE PRECISION,
    description     TEXT,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    version         BIGINT NOT NULL DEFAULT 0,
    UNIQUE(unit_number, estate_id)
);

CREATE INDEX idx_units_tenant_id ON units(tenant_id);
CREATE INDEX idx_units_estate_id ON units(estate_id);
CREATE INDEX idx_units_status ON units(tenant_id, status) WHERE is_deleted = FALSE;
