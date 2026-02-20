-- V7: Create amenity, maintenance, and utility tables

-- Amenities
CREATE TABLE amenities (
    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id                   UUID NOT NULL,
    estate_id                   UUID NOT NULL REFERENCES estates(id),
    name                        VARCHAR(255) NOT NULL,
    description                 TEXT,
    amenity_type                VARCHAR(50) NOT NULL,
    status                      VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    capacity                    INT,
    price_per_hour              DECIMAL(15,2),
    price_per_session           DECIMAL(15,2),
    requires_booking            BOOLEAN NOT NULL DEFAULT TRUE,
    max_booking_duration_hours  INT,
    min_booking_duration_hours  INT,
    advance_booking_days        INT,
    cancellation_hours_before   INT,
    opening_time                TIME,
    closing_time                TIME,
    operating_days              VARCHAR(100),
    rules                       JSONB,
    photo_urls                  JSONB,
    contact_info                VARCHAR(500),
    active                      BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted                  BOOLEAN NOT NULL DEFAULT FALSE,
    version                     BIGINT NOT NULL DEFAULT 0,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ,
    created_by                  VARCHAR(255),
    updated_by                  VARCHAR(255)
);

CREATE INDEX idx_amenities_tenant_id ON amenities(tenant_id);
CREATE INDEX idx_amenities_estate_id ON amenities(estate_id, tenant_id) WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX idx_amenities_name_estate_tenant ON amenities(name, estate_id, tenant_id) WHERE is_deleted = FALSE;

-- Amenity Bookings
CREATE TABLE amenity_bookings (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id               UUID NOT NULL,
    booking_reference       VARCHAR(50) NOT NULL,
    amenity_id              UUID NOT NULL REFERENCES amenities(id),
    resident_id             UUID NOT NULL REFERENCES residents(id),
    start_time              TIMESTAMPTZ NOT NULL,
    end_time                TIMESTAMPTZ NOT NULL,
    status                  VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    amount_charged          DECIMAL(15,2),
    amount_paid             DECIMAL(15,2),
    payment_reference       VARCHAR(255),
    number_of_guests        INT,
    purpose                 VARCHAR(500),
    notes                   TEXT,
    recurring               BOOLEAN NOT NULL DEFAULT FALSE,
    recurrence_pattern      VARCHAR(50),
    recurrence_end_date     DATE,
    parent_booking_id       UUID REFERENCES amenity_bookings(id),
    cancelled_at            TIMESTAMPTZ,
    cancellation_reason     VARCHAR(500),
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    version                 BIGINT NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ,
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255)
);

CREATE INDEX idx_amenity_bookings_tenant_id ON amenity_bookings(tenant_id);
CREATE INDEX idx_amenity_bookings_amenity_id ON amenity_bookings(amenity_id, tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_amenity_bookings_resident_id ON amenity_bookings(resident_id, tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_amenity_bookings_status ON amenity_bookings(status, tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_amenity_bookings_time ON amenity_bookings(amenity_id, start_time, end_time) WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX idx_amenity_bookings_reference ON amenity_bookings(booking_reference, tenant_id) WHERE is_deleted = FALSE;

-- Booking Waitlist
CREATE TABLE booking_waitlist (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL,
    amenity_id          UUID NOT NULL REFERENCES amenities(id),
    resident_id         UUID NOT NULL REFERENCES residents(id),
    desired_start_time  TIMESTAMPTZ NOT NULL,
    desired_end_time    TIMESTAMPTZ NOT NULL,
    notified            BOOLEAN NOT NULL DEFAULT FALSE,
    notified_at         TIMESTAMPTZ,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255)
);

CREATE INDEX idx_booking_waitlist_tenant_id ON booking_waitlist(tenant_id);
CREATE INDEX idx_booking_waitlist_amenity_id ON booking_waitlist(amenity_id, tenant_id) WHERE is_deleted = FALSE;

-- Maintenance Requests
CREATE TABLE maintenance_requests (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id               UUID NOT NULL,
    request_number          VARCHAR(50) NOT NULL,
    unit_id                 UUID REFERENCES units(id),
    estate_id               UUID NOT NULL REFERENCES estates(id),
    resident_id             UUID NOT NULL REFERENCES residents(id),
    title                   VARCHAR(500) NOT NULL,
    description             TEXT NOT NULL,
    category                VARCHAR(50) NOT NULL,
    priority                VARCHAR(50) NOT NULL,
    status                  VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    assigned_to             VARCHAR(255),
    assigned_to_phone       VARCHAR(50),
    assigned_at             TIMESTAMPTZ,
    photo_urls              JSONB,
    estimated_cost          DECIMAL(15,2),
    actual_cost             DECIMAL(15,2),
    cost_approved_by        VARCHAR(255),
    cost_approved_at        TIMESTAMPTZ,
    sla_deadline            TIMESTAMPTZ,
    sla_breached            BOOLEAN NOT NULL DEFAULT FALSE,
    escalated               BOOLEAN NOT NULL DEFAULT FALSE,
    escalated_at            TIMESTAMPTZ,
    resolved_at             TIMESTAMPTZ,
    closed_at               TIMESTAMPTZ,
    resolution_notes        TEXT,
    satisfaction_rating     INT,
    satisfaction_comment    TEXT,
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    version                 BIGINT NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ,
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255)
);

CREATE INDEX idx_maintenance_requests_tenant_id ON maintenance_requests(tenant_id);
CREATE INDEX idx_maintenance_requests_unit_id ON maintenance_requests(unit_id, tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_maintenance_requests_estate_id ON maintenance_requests(estate_id, tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_maintenance_requests_resident_id ON maintenance_requests(resident_id, tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_maintenance_requests_status ON maintenance_requests(status, tenant_id) WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX idx_maintenance_requests_number ON maintenance_requests(request_number, tenant_id) WHERE is_deleted = FALSE;

-- Maintenance Comments
CREATE TABLE maintenance_comments (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL,
    request_id          UUID NOT NULL REFERENCES maintenance_requests(id),
    author_id           UUID NOT NULL,
    author_name         VARCHAR(255) NOT NULL,
    author_role         VARCHAR(100) NOT NULL,
    content             TEXT NOT NULL,
    attachment_urls     JSONB,
    is_internal         BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted          BOOLEAN NOT NULL DEFAULT FALSE,
    version             BIGINT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255)
);

CREATE INDEX idx_maintenance_comments_tenant_id ON maintenance_comments(tenant_id);
CREATE INDEX idx_maintenance_comments_request_id ON maintenance_comments(request_id, tenant_id) WHERE is_deleted = FALSE;

-- Utility Meters
CREATE TABLE utility_meters (
    id                              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id                       UUID NOT NULL,
    meter_number                    VARCHAR(100) NOT NULL,
    unit_id                         UUID REFERENCES units(id),
    estate_id                       UUID NOT NULL REFERENCES estates(id),
    utility_type                    VARCHAR(50) NOT NULL,
    meter_type                      VARCHAR(50) NOT NULL,
    rate_per_unit                   DECIMAL(15,4),
    unit_of_measure                 VARCHAR(50),
    consumption_alert_threshold     DOUBLE PRECISION,
    last_reading_value              DOUBLE PRECISION,
    last_reading_date               DATE,
    active                          BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted                      BOOLEAN NOT NULL DEFAULT FALSE,
    version                         BIGINT NOT NULL DEFAULT 0,
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMPTZ,
    created_by                      VARCHAR(255),
    updated_by                      VARCHAR(255)
);

CREATE INDEX idx_utility_meters_tenant_id ON utility_meters(tenant_id);
CREATE INDEX idx_utility_meters_unit_id ON utility_meters(unit_id, tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_utility_meters_estate_id ON utility_meters(estate_id, tenant_id) WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX idx_utility_meters_number_tenant ON utility_meters(meter_number, tenant_id) WHERE is_deleted = FALSE;

-- Utility Readings
CREATE TABLE utility_readings (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id               UUID NOT NULL,
    meter_id                UUID NOT NULL REFERENCES utility_meters(id),
    unit_id                 UUID,
    utility_type            VARCHAR(50) NOT NULL,
    previous_reading        DOUBLE PRECISION NOT NULL,
    current_reading         DOUBLE PRECISION NOT NULL,
    consumption             DOUBLE PRECISION NOT NULL,
    rate_per_unit           DECIMAL(15,4),
    cost                    DECIMAL(15,2),
    billing_period_start    DATE,
    billing_period_end      DATE,
    reading_date            DATE NOT NULL,
    status                  VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    invoice_id              UUID,
    notes                   TEXT,
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    version                 BIGINT NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ,
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255)
);

CREATE INDEX idx_utility_readings_tenant_id ON utility_readings(tenant_id);
CREATE INDEX idx_utility_readings_meter_id ON utility_readings(meter_id, tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_utility_readings_unit_id ON utility_readings(unit_id, tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_utility_readings_status ON utility_readings(status, tenant_id) WHERE is_deleted = FALSE;

-- Shared Utility Costs
CREATE TABLE shared_utility_costs (
    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id                   UUID NOT NULL,
    estate_id                   UUID NOT NULL REFERENCES estates(id),
    utility_type                VARCHAR(50) NOT NULL,
    total_cost                  DECIMAL(15,2) NOT NULL,
    split_method                VARCHAR(50) NOT NULL,
    total_units_participating   INT,
    cost_per_unit               DECIMAL(15,2),
    billing_period_start        DATE NOT NULL,
    billing_period_end          DATE NOT NULL,
    description                 TEXT,
    invoices_generated          BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted                  BOOLEAN NOT NULL DEFAULT FALSE,
    version                     BIGINT NOT NULL DEFAULT 0,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ,
    created_by                  VARCHAR(255),
    updated_by                  VARCHAR(255)
);

CREATE INDEX idx_shared_utility_costs_tenant_id ON shared_utility_costs(tenant_id);
CREATE INDEX idx_shared_utility_costs_estate_id ON shared_utility_costs(estate_id, tenant_id) WHERE is_deleted = FALSE;
