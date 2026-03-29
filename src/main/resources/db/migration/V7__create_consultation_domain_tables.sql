-- Client consultation config (per-client billing settings)
CREATE TABLE client_consultation_configs (
    client_id       UUID PRIMARY KEY REFERENCES clients(id),
    included_hours  DECIMAL(10,2) NOT NULL,
    excess_rate     DECIMAL(15,2) NOT NULL
);

-- Consultation logs
CREATE TABLE consultation_logs (
    id              UUID PRIMARY KEY,
    version         BIGINT        NOT NULL DEFAULT 0,
    client_id       UUID          NOT NULL REFERENCES clients(id),
    date            DATE          NOT NULL,
    start_time      TIME          NOT NULL,
    end_time        TIME          NOT NULL,
    hours           DECIMAL(10,2) NOT NULL,
    platform        VARCHAR(255),
    subject         VARCHAR(500)  NOT NULL,
    notes           JSONB,
    attachments     JSONB,
    billable_type   VARCHAR(50)   NOT NULL CHECK (billable_type IN ('INCLUDED', 'EXCESS', 'COURTESY')),
    status          VARCHAR(50)   NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED')),
    created_by      UUID          NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_cl_client_id       ON consultation_logs(client_id);
CREATE INDEX idx_cl_status          ON consultation_logs(status);
CREATE INDEX idx_cl_client_status   ON consultation_logs(client_id, status);
CREATE INDEX idx_cl_client_date     ON consultation_logs(client_id, date);
CREATE INDEX idx_cl_created_by      ON consultation_logs(created_by);

-- Consultation log audits
CREATE TABLE consultation_log_audits (
    id                  UUID PRIMARY KEY,
    consultation_log_id UUID         NOT NULL REFERENCES consultation_logs(id) ON DELETE CASCADE,
    action              VARCHAR(50)  NOT NULL CHECK (action IN ('CREATED', 'SUBMITTED', 'APPROVED', 'REJECTED')),
    comment             JSONB,
    performed_by        UUID         NOT NULL REFERENCES users(id),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_cla_log_id ON consultation_log_audits(consultation_log_id);
