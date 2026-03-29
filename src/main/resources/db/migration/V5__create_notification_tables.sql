-- Notifications
CREATE TABLE notifications (
    id              UUID PRIMARY KEY,
    recipient_id    UUID         NOT NULL REFERENCES users(id),
    type            VARCHAR(50)  NOT NULL CHECK (type IN ('TASK_ASSIGNED', 'TASK_SUBMITTED', 'TASK_APPROVED', 'TASK_REJECTED', 'TASK_FILED', 'TASK_COMPLETED', 'CLIENT_HANDOFF', 'OFFBOARDING_ASSIGNED', 'PROFILE_SUBMITTED', 'PROFILE_REJECTED', 'PROFILE_APPROVED', 'CONSULTATION_SUBMITTED', 'CONSULTATION_APPROVED', 'CONSULTATION_REJECTED')),
    reference_id    UUID         NOT NULL,
    reference_type  VARCHAR(50)  NOT NULL CHECK (reference_type IN ('TASK', 'TAX_RECORD_TASK', 'CLIENT', 'CLIENT_INFO', 'CLIENT_INFO_EDIT', 'CONSULTATION_LOG')),
    message         TEXT         NOT NULL,
    is_read         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_notif_recipient_read_created ON notifications(recipient_id, is_read, created_at DESC);
