-- Notifications
CREATE TABLE notifications (
    id              UUID PRIMARY KEY,
    recipient_id    UUID         NOT NULL REFERENCES users(id),
    type            VARCHAR(255) NOT NULL,
    reference_id    UUID         NOT NULL,
    reference_type  VARCHAR(255) NOT NULL,
    message         TEXT         NOT NULL,
    is_read         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_notif_recipient_read_created ON notifications(recipient_id, is_read, created_at DESC);
