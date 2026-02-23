-- V14: Refactor billing - split charges into tenant/estate types, add reminders

-- 1. Rename levy_types -> estate_charges
ALTER TABLE levy_types RENAME TO estate_charges;

-- Rename indexes
ALTER INDEX idx_levy_types_tenant_id RENAME TO idx_estate_charges_tenant_id;
ALTER INDEX idx_levy_types_estate_id RENAME TO idx_estate_charges_estate_id;
ALTER INDEX idx_levy_types_name_estate_tenant RENAME TO idx_estate_charges_name_estate_tenant;

-- Add reminder_days_before to estate_charges
ALTER TABLE estate_charges ADD COLUMN reminder_days_before JSONB;

-- 2. Create tenant_charges table
CREATE TABLE tenant_charges (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id             UUID NOT NULL,
    tenancy_id            UUID NOT NULL REFERENCES tenancies(id),
    estate_id             UUID NOT NULL REFERENCES estates(id),
    name                  VARCHAR(255) NOT NULL,
    description           TEXT,
    amount                DECIMAL(15,2) NOT NULL,
    frequency             VARCHAR(50) NOT NULL,
    category              VARCHAR(100),
    reminder_days_before  JSONB,
    active                BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted            BOOLEAN NOT NULL DEFAULT FALSE,
    version               BIGINT NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ,
    created_by            VARCHAR(255),
    updated_by            VARCHAR(255)
);

CREATE INDEX idx_tenant_charges_tenant_id ON tenant_charges(tenant_id);
CREATE INDEX idx_tenant_charges_tenancy_id ON tenant_charges(tenancy_id);
CREATE INDEX idx_tenant_charges_estate_id ON tenant_charges(estate_id);
CREATE UNIQUE INDEX idx_tenant_charges_name_tenancy_tenant
    ON tenant_charges(name, tenancy_id, tenant_id) WHERE is_deleted = FALSE;

-- 3. Create estate_charge_exclusions table
CREATE TABLE estate_charge_exclusions (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL,
    estate_charge_id    UUID NOT NULL REFERENCES estate_charges(id),
    tenancy_id          UUID NOT NULL REFERENCES tenancies(id),
    reason              TEXT,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255)
);

CREATE INDEX idx_estate_charge_exclusions_tenant_id ON estate_charge_exclusions(tenant_id);
CREATE INDEX idx_estate_charge_exclusions_charge_id ON estate_charge_exclusions(estate_charge_id);
CREATE INDEX idx_estate_charge_exclusions_tenancy_id ON estate_charge_exclusions(tenancy_id);
CREATE UNIQUE INDEX idx_estate_charge_exclusion_unique
    ON estate_charge_exclusions(estate_charge_id, tenancy_id, tenant_id) WHERE is_deleted = FALSE;

-- 4. Rename levy_invoices -> charge_invoices
ALTER TABLE levy_invoices RENAME TO charge_invoices;

-- Rename indexes
ALTER INDEX idx_levy_invoices_tenant_id RENAME TO idx_charge_invoices_tenant_id;
ALTER INDEX idx_levy_invoices_unit_id RENAME TO idx_charge_invoices_unit_id;
ALTER INDEX idx_levy_invoices_resident_id RENAME TO idx_charge_invoices_resident_id;
ALTER INDEX idx_levy_invoices_status RENAME TO idx_charge_invoices_status;
ALTER INDEX idx_levy_invoices_due_date RENAME TO idx_charge_invoices_due_date;
ALTER INDEX idx_levy_invoices_invoice_number_tenant RENAME TO idx_charge_invoices_invoice_number_tenant;

-- Rename levy_type_id -> charge_id
ALTER TABLE charge_invoices RENAME COLUMN levy_type_id TO charge_id;

-- Add charge_type column (default ESTATE_CHARGE for all existing data)
ALTER TABLE charge_invoices ADD COLUMN charge_type VARCHAR(50) NOT NULL DEFAULT 'ESTATE_CHARGE';

-- Add index on charge_type
CREATE INDEX idx_charge_invoices_charge_type ON charge_invoices(charge_type);

-- 5. Create charge_reminder_logs table
CREATE TABLE charge_reminder_logs (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL,
    invoice_id      UUID NOT NULL REFERENCES charge_invoices(id),
    days_before     INTEGER NOT NULL,
    sent_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);

CREATE INDEX idx_charge_reminder_logs_tenant_id ON charge_reminder_logs(tenant_id);
CREATE INDEX idx_charge_reminder_logs_invoice_id ON charge_reminder_logs(invoice_id);
CREATE UNIQUE INDEX idx_charge_reminder_log_unique
    ON charge_reminder_logs(invoice_id, days_before, tenant_id) WHERE is_deleted = FALSE;
