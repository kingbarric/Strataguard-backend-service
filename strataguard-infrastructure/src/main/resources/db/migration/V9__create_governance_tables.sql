-- =============================================
-- Phase 7: Governance & Community Tables
-- =============================================

-- Artisan/Vendor Registry
CREATE TABLE artisans (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    estate_id       UUID NOT NULL REFERENCES estates(id),
    name            VARCHAR(255) NOT NULL,
    phone           VARCHAR(50) NOT NULL,
    email           VARCHAR(255),
    category        VARCHAR(50) NOT NULL,
    specialization  VARCHAR(255),
    description     TEXT,
    photo_url       VARCHAR(500),
    address         VARCHAR(500),
    status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    verified        BOOLEAN NOT NULL DEFAULT FALSE,
    total_jobs      INTEGER NOT NULL DEFAULT 0,
    total_rating    DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    rating_count    INTEGER NOT NULL DEFAULT 0,
    average_rating  DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    version         BIGINT DEFAULT 0
);

CREATE INDEX idx_artisan_tenant_id ON artisans(tenant_id);
CREATE INDEX idx_artisan_estate_id ON artisans(estate_id);
CREATE INDEX idx_artisan_category ON artisans(category);
CREATE INDEX idx_artisan_status ON artisans(status);

-- Artisan Ratings
CREATE TABLE artisan_ratings (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    artisan_id              UUID NOT NULL REFERENCES artisans(id),
    resident_id             UUID NOT NULL REFERENCES residents(id),
    maintenance_request_id  UUID REFERENCES maintenance_requests(id),
    rating                  INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review                  TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ,
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255),
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    version                 BIGINT DEFAULT 0
);

CREATE INDEX idx_artisan_rating_tenant_id ON artisan_ratings(tenant_id);
CREATE INDEX idx_artisan_rating_artisan_id ON artisan_ratings(artisan_id);
CREATE INDEX idx_artisan_rating_resident_id ON artisan_ratings(resident_id);

-- Announcements
CREATE TABLE announcements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    estate_id       UUID NOT NULL REFERENCES estates(id),
    title           VARCHAR(500) NOT NULL,
    body            TEXT NOT NULL,
    audience        VARCHAR(30) NOT NULL,
    audience_filter VARCHAR(500),
    priority        VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    posted_by       VARCHAR(255) NOT NULL,
    posted_by_name  VARCHAR(255),
    published_at    TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ,
    is_pinned       BOOLEAN NOT NULL DEFAULT FALSE,
    is_published    BOOLEAN NOT NULL DEFAULT FALSE,
    attachment_url  VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    version         BIGINT DEFAULT 0
);

CREATE INDEX idx_announcement_tenant_id ON announcements(tenant_id);
CREATE INDEX idx_announcement_estate_id ON announcements(estate_id);
CREATE INDEX idx_announcement_audience ON announcements(audience);
CREATE INDEX idx_announcement_published_at ON announcements(published_at);

-- Polls
CREATE TABLE polls (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    estate_id               UUID NOT NULL REFERENCES estates(id),
    title                   VARCHAR(500) NOT NULL,
    description             TEXT,
    status                  VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by_user_id      VARCHAR(255) NOT NULL,
    created_by_name         VARCHAR(255),
    starts_at               TIMESTAMPTZ,
    deadline                TIMESTAMPTZ NOT NULL,
    allow_multiple_choices  BOOLEAN NOT NULL DEFAULT FALSE,
    is_anonymous            BOOLEAN NOT NULL DEFAULT FALSE,
    allow_proxy_voting      BOOLEAN NOT NULL DEFAULT FALSE,
    total_votes             INTEGER NOT NULL DEFAULT 0,
    eligible_voter_count    INTEGER,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ,
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255),
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    version                 BIGINT DEFAULT 0
);

CREATE INDEX idx_poll_tenant_id ON polls(tenant_id);
CREATE INDEX idx_poll_estate_id ON polls(estate_id);
CREATE INDEX idx_poll_status ON polls(status);

-- Poll Options
CREATE TABLE poll_options (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    poll_id         UUID NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
    option_text     VARCHAR(500) NOT NULL,
    vote_count      INTEGER NOT NULL DEFAULT 0,
    display_order   INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    version         BIGINT DEFAULT 0
);

CREATE INDEX idx_poll_option_tenant_id ON poll_options(tenant_id);
CREATE INDEX idx_poll_option_poll_id ON poll_options(poll_id);

-- Poll Votes
CREATE TABLE poll_votes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    poll_id         UUID NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
    option_id       UUID NOT NULL REFERENCES poll_options(id) ON DELETE CASCADE,
    voter_id        UUID NOT NULL REFERENCES residents(id),
    proxy_for_id    UUID REFERENCES residents(id),
    is_proxy_vote   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ,
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    version         BIGINT DEFAULT 0,
    CONSTRAINT uk_poll_vote_voter_option UNIQUE (poll_id, voter_id, option_id, tenant_id)
);

CREATE INDEX idx_poll_vote_tenant_id ON poll_votes(tenant_id);
CREATE INDEX idx_poll_vote_poll_id ON poll_votes(poll_id);
CREATE INDEX idx_poll_vote_voter_id ON poll_votes(voter_id);

-- Violations
CREATE TABLE violations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    estate_id           UUID NOT NULL REFERENCES estates(id),
    unit_id             UUID NOT NULL REFERENCES units(id),
    resident_id         UUID REFERENCES residents(id),
    rule_violated       VARCHAR(500) NOT NULL,
    description         TEXT NOT NULL,
    fine_amount         DECIMAL(15, 2),
    status              VARCHAR(30) NOT NULL DEFAULT 'REPORTED',
    reported_by         VARCHAR(255) NOT NULL,
    reported_by_name    VARCHAR(255),
    evidence_url        VARCHAR(500),
    resolution_notes    TEXT,
    resolved_at         TIMESTAMPTZ,
    appeal_reason       TEXT,
    appealed_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    version             BIGINT DEFAULT 0
);

CREATE INDEX idx_violation_tenant_id ON violations(tenant_id);
CREATE INDEX idx_violation_estate_id ON violations(estate_id);
CREATE INDEX idx_violation_unit_id ON violations(unit_id);
CREATE INDEX idx_violation_status ON violations(status);

-- Complaints/Petitions
CREATE TABLE complaints (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    estate_id           UUID NOT NULL REFERENCES estates(id),
    resident_id         UUID NOT NULL REFERENCES residents(id),
    title               VARCHAR(500) NOT NULL,
    description         TEXT NOT NULL,
    category            VARCHAR(50) NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    is_anonymous        BOOLEAN NOT NULL DEFAULT FALSE,
    assigned_to         VARCHAR(255),
    assigned_to_name    VARCHAR(255),
    response_notes      TEXT,
    resolved_at         TIMESTAMPTZ,
    attachment_url      VARCHAR(500),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    version             BIGINT DEFAULT 0
);

CREATE INDEX idx_complaint_tenant_id ON complaints(tenant_id);
CREATE INDEX idx_complaint_estate_id ON complaints(estate_id);
CREATE INDEX idx_complaint_resident_id ON complaints(resident_id);
CREATE INDEX idx_complaint_status ON complaints(status);
