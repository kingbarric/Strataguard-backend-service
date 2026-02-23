-- =============================================
-- Chat Module Tables
-- =============================================

-- Chat Conversations
CREATE TABLE chat_conversations (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    estate_id               UUID NOT NULL REFERENCES estates(id),
    title                   VARCHAR(255),
    type                    VARCHAR(20) NOT NULL,
    last_message_at         TIMESTAMPTZ,
    last_message_preview    VARCHAR(500),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ,
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255),
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    version                 BIGINT DEFAULT 0
);

CREATE INDEX idx_chat_conversation_tenant_id ON chat_conversations(tenant_id);
CREATE INDEX idx_chat_conversation_estate_id ON chat_conversations(estate_id);
CREATE INDEX idx_chat_conversation_type ON chat_conversations(type);

-- Chat Participants
CREATE TABLE chat_participants (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    conversation_id         UUID NOT NULL REFERENCES chat_conversations(id) ON DELETE CASCADE,
    resident_id             UUID NOT NULL REFERENCES residents(id),
    role                    VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_read_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ,
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255),
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    version                 BIGINT DEFAULT 0,
    CONSTRAINT uk_chat_participant_conv_resident UNIQUE (conversation_id, resident_id, tenant_id)
);

CREATE INDEX idx_chat_participant_tenant_id ON chat_participants(tenant_id);
CREATE INDEX idx_chat_participant_conversation_id ON chat_participants(conversation_id);
CREATE INDEX idx_chat_participant_resident_id ON chat_participants(resident_id);

-- Chat Messages
CREATE TABLE chat_messages (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    conversation_id         UUID NOT NULL REFERENCES chat_conversations(id) ON DELETE CASCADE,
    sender_id               UUID NOT NULL REFERENCES residents(id),
    content                 TEXT NOT NULL,
    message_type            VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    status                  VARCHAR(20) NOT NULL DEFAULT 'SENT',
    parent_message_id       UUID REFERENCES chat_messages(id),
    attachment_url          VARCHAR(500),
    sender_name             VARCHAR(255),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ,
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255),
    is_deleted              BOOLEAN NOT NULL DEFAULT FALSE,
    version                 BIGINT DEFAULT 0
);

CREATE INDEX idx_chat_message_tenant_id ON chat_messages(tenant_id);
CREATE INDEX idx_chat_message_conversation_id ON chat_messages(conversation_id);
CREATE INDEX idx_chat_message_sender_id ON chat_messages(sender_id);
CREATE INDEX idx_chat_message_created_at ON chat_messages(created_at);
