-- V5: Create billing and payment tables

-- Levy Types
CREATE TABLE levy_types (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL,
    estate_id       UUID NOT NULL REFERENCES estates(id),
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    amount          DECIMAL(15,2) NOT NULL,
    frequency       VARCHAR(50) NOT NULL,
    category        VARCHAR(100),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255)
);

CREATE INDEX idx_levy_types_tenant_id ON levy_types(tenant_id);
CREATE INDEX idx_levy_types_estate_id ON levy_types(estate_id);
CREATE UNIQUE INDEX idx_levy_types_name_estate_tenant ON levy_types(name, estate_id, tenant_id) WHERE is_deleted = FALSE;

-- Levy Invoices
CREATE TABLE levy_invoices (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id             UUID NOT NULL,
    invoice_number        VARCHAR(50) NOT NULL,
    levy_type_id          UUID NOT NULL REFERENCES levy_types(id),
    unit_id               UUID NOT NULL REFERENCES units(id),
    resident_id           UUID REFERENCES residents(id),
    amount                DECIMAL(15,2) NOT NULL,
    penalty_amount        DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_amount          DECIMAL(15,2) NOT NULL,
    paid_amount           DECIMAL(15,2) NOT NULL DEFAULT 0,
    due_date              DATE NOT NULL,
    status                VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    billing_period_start  DATE,
    billing_period_end    DATE,
    notes                 TEXT,
    active                BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted            BOOLEAN NOT NULL DEFAULT FALSE,
    version               BIGINT NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ,
    created_by            VARCHAR(255),
    updated_by            VARCHAR(255)
);

CREATE INDEX idx_levy_invoices_tenant_id ON levy_invoices(tenant_id);
CREATE INDEX idx_levy_invoices_unit_id ON levy_invoices(unit_id);
CREATE INDEX idx_levy_invoices_resident_id ON levy_invoices(resident_id);
CREATE INDEX idx_levy_invoices_status ON levy_invoices(tenant_id, status) WHERE is_deleted = FALSE;
CREATE INDEX idx_levy_invoices_due_date ON levy_invoices(due_date);
CREATE UNIQUE INDEX idx_levy_invoices_invoice_number_tenant ON levy_invoices(invoice_number, tenant_id) WHERE is_deleted = FALSE;

-- Payments
CREATE TABLE payments (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL,
    invoice_id          UUID NOT NULL REFERENCES levy_invoices(id),
    amount              DECIMAL(15,2) NOT NULL,
    payment_method      VARCHAR(50) NOT NULL,
    payment_provider    VARCHAR(50) NOT NULL,
    reference           VARCHAR(255) NOT NULL,
    provider_reference  VARCHAR(255),
    status              VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    paid_at             TIMESTAMPTZ,
    metadata            JSONB,
    notes               TEXT,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255)
);

CREATE INDEX idx_payments_tenant_id ON payments(tenant_id);
CREATE INDEX idx_payments_invoice_id ON payments(invoice_id);
CREATE UNIQUE INDEX idx_payments_reference ON payments(reference) WHERE is_deleted = FALSE;

-- Resident Wallets
CREATE TABLE resident_wallets (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL,
    resident_id     UUID NOT NULL,
    balance         DECIMAL(15,2) NOT NULL DEFAULT 0,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    CONSTRAINT uq_wallet_resident_tenant UNIQUE (resident_id, tenant_id)
);

CREATE INDEX idx_resident_wallets_tenant_id ON resident_wallets(tenant_id);
CREATE INDEX idx_resident_wallets_resident_id ON resident_wallets(resident_id);

-- Wallet Transactions
CREATE TABLE wallet_transactions (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id         UUID NOT NULL,
    wallet_id         UUID NOT NULL REFERENCES resident_wallets(id),
    amount            DECIMAL(15,2) NOT NULL,
    transaction_type  VARCHAR(50) NOT NULL,
    reference_id      UUID,
    reference_type    VARCHAR(50),
    description       TEXT,
    balance_after     DECIMAL(15,2) NOT NULL,
    is_deleted        BOOLEAN NOT NULL DEFAULT FALSE,
    version           BIGINT NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ,
    created_by        VARCHAR(255),
    updated_by        VARCHAR(255)
);

CREATE INDEX idx_wallet_transactions_tenant_id ON wallet_transactions(tenant_id);
CREATE INDEX idx_wallet_transactions_wallet_id ON wallet_transactions(wallet_id);
