-- Clients
CREATE TABLE clients (
    id            UUID PRIMARY KEY,
    version       BIGINT       NOT NULL DEFAULT 0,
    status        VARCHAR(255) NOT NULL,
    handed_off    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by    UUID         NOT NULL REFERENCES users(id),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ
);

CREATE INDEX idx_clients_status     ON clients(status);
CREATE INDEX idx_clients_created_by ON clients(created_by);

-- Add FK from users.client_id -> clients.id (deferred because clients table didn't exist in V1)
ALTER TABLE users ADD CONSTRAINT fk_users_client_id FOREIGN KEY (client_id) REFERENCES clients(id);

-- Client-Accountant join table
CREATE TABLE client_accountants (
    client_id UUID NOT NULL REFERENCES clients(id),
    user_id   UUID NOT NULL REFERENCES users(id),
    PRIMARY KEY (client_id, user_id)
);

CREATE INDEX idx_client_accountants_client_id ON client_accountants(client_id);
CREATE INDEX idx_client_accountants_user_id   ON client_accountants(user_id);

-- Client info
CREATE TABLE client_info (
    id                             UUID PRIMARY KEY,
    client_id                      UUID NOT NULL UNIQUE REFERENCES clients(id),
    main_details                   JSONB,
    client_information             JSONB,
    corporate_officer_information  JSONB,
    access_credentials             JSONB,
    scope_of_engagement            JSONB,
    professional_fees              JSONB,
    onboarding_details             JSONB
);

-- Client info templates
CREATE TABLE client_info_templates (
    id                             SERIAL PRIMARY KEY,
    main_details                   JSONB NOT NULL,
    client_information             JSONB NOT NULL,
    corporate_officer_information  JSONB NOT NULL,
    access_credentials             JSONB NOT NULL,
    scope_of_engagement            JSONB NOT NULL,
    professional_fees              JSONB NOT NULL,
    onboarding_details             JSONB NOT NULL
);

-- Client info snapshots
CREATE TABLE client_info_snapshots (
    id                             UUID PRIMARY KEY,
    client_id                      UUID NOT NULL REFERENCES clients(id),
    created_by                     UUID NOT NULL REFERENCES users(id),
    created_at                     TIMESTAMPTZ NOT NULL DEFAULT now(),
    main_details                   JSONB,
    client_information             JSONB,
    corporate_officer_information  JSONB,
    access_credentials             JSONB,
    scope_of_engagement            JSONB,
    professional_fees              JSONB,
    onboarding_details             JSONB
);

CREATE INDEX idx_snapshots_client_id ON client_info_snapshots(client_id);

-- Client info tasks
CREATE TABLE client_info_tasks (
    id                             UUID PRIMARY KEY,
    version                        BIGINT NOT NULL DEFAULT 0,
    client_id                      UUID NOT NULL REFERENCES clients(id),
    type                           VARCHAR(255) NOT NULL,
    status                         VARCHAR(255) NOT NULL,
    main_details                   JSONB,
    client_information             JSONB,
    corporate_officer_information  JSONB,
    access_credentials             JSONB,
    scope_of_engagement            JSONB,
    professional_fees              JSONB,
    onboarding_details             JSONB,
    changed_section_keys           JSONB,
    approved_diff                  JSONB,
    submitted_by                   UUID REFERENCES users(id),
    submitted_at                   TIMESTAMPTZ,
    created_by                     UUID NOT NULL REFERENCES users(id),
    created_at                     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                     TIMESTAMPTZ
);

CREATE INDEX idx_cit_client_type_status ON client_info_tasks(client_id, type, status);
CREATE INDEX idx_cit_submitted_at       ON client_info_tasks(submitted_at);
CREATE INDEX idx_cit_submitted_by       ON client_info_tasks(submitted_by);

-- Client info task logs
CREATE TABLE client_info_task_logs (
    id            UUID PRIMARY KEY,
    task_id       UUID         NOT NULL REFERENCES client_info_tasks(id),
    action        VARCHAR(255) NOT NULL,
    comment       TEXT,
    performed_by  UUID         NOT NULL REFERENCES users(id),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_cit_logs_task_id ON client_info_task_logs(task_id);

-- Client notices
CREATE TABLE client_notices (
    id        SERIAL PRIMARY KEY,
    client_id UUID         NOT NULL REFERENCES clients(id),
    type      VARCHAR(255) NOT NULL,
    content   TEXT         NOT NULL
);

CREATE INDEX idx_client_notices_client_id ON client_notices(client_id);

-- Tax record entries (depends on task_domain lookup tables, but the FKs will be added in V3/V4)
CREATE TABLE tax_record_entries (
    id                      UUID PRIMARY KEY,
    client_id               UUID NOT NULL REFERENCES clients(id),
    category_id             INT  NOT NULL,
    sub_category_id         INT  NOT NULL,
    task_name_id            INT  NOT NULL,
    year                    INT  NOT NULL,
    period                  VARCHAR(255) NOT NULL,
    working_files           JSONB,
    output_file_id          UUID,
    proof_of_filing_file_id UUID,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_tre_client_id ON tax_record_entries(client_id);
