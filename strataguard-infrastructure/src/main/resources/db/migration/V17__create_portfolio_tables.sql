-- =============================================
-- V17: Portfolio tables and estate FK
-- =============================================

-- 1. portfolios table
CREATE TABLE portfolios (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    logo_url        VARCHAR(500),
    contact_email   VARCHAR(255),
    contact_phone   VARCHAR(50),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_portfolios_tenant_id ON portfolios(tenant_id);
CREATE INDEX idx_portfolios_name ON portfolios(name);
CREATE UNIQUE INDEX idx_portfolios_name_tenant ON portfolios(name, tenant_id) WHERE is_deleted = FALSE;

-- 2. portfolio_memberships table
CREATE TABLE portfolio_memberships (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL,
    user_id         VARCHAR(255) NOT NULL,
    portfolio_id    UUID NOT NULL REFERENCES portfolios(id),
    role            VARCHAR(50) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_pm_tenant_id ON portfolio_memberships(tenant_id);
CREATE INDEX idx_pm_user_id ON portfolio_memberships(user_id);
CREATE INDEX idx_pm_portfolio_id ON portfolio_memberships(portfolio_id);
CREATE UNIQUE INDEX idx_pm_user_portfolio_unique ON portfolio_memberships(user_id, portfolio_id, tenant_id) WHERE is_deleted = FALSE;

-- 3. Add portfolio_id FK to estates
ALTER TABLE estates ADD COLUMN portfolio_id UUID REFERENCES portfolios(id);
CREATE INDEX idx_estates_portfolio_id ON estates(portfolio_id);
