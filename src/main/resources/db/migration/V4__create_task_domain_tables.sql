-- Tax task lookup hierarchy
CREATE TABLE tax_task_categories (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE tax_task_sub_categories (
    id          SERIAL PRIMARY KEY,
    category_id INT          NOT NULL REFERENCES tax_task_categories(id),
    name        VARCHAR(255) NOT NULL
);

CREATE TABLE tax_task_names (
    id              SERIAL PRIMARY KEY,
    sub_category_id INT          NOT NULL REFERENCES tax_task_sub_categories(id),
    name            VARCHAR(255) NOT NULL
);

-- Add lookup FKs to tax_record_entries (created in V2)
ALTER TABLE tax_record_entries
    ADD CONSTRAINT fk_tre_category FOREIGN KEY (category_id) REFERENCES tax_task_categories(id),
    ADD CONSTRAINT fk_tre_sub_category FOREIGN KEY (sub_category_id) REFERENCES tax_task_sub_categories(id),
    ADD CONSTRAINT fk_tre_task_name FOREIGN KEY (task_name_id) REFERENCES tax_task_names(id);

-- Tax record tasks
CREATE TABLE tax_record_tasks (
    id                      UUID PRIMARY KEY,
    version                 BIGINT       NOT NULL DEFAULT 0,
    client_id               UUID         NOT NULL REFERENCES clients(id),
    category_id             INT          NOT NULL REFERENCES tax_task_categories(id),
    sub_category_id         INT          NOT NULL REFERENCES tax_task_sub_categories(id),
    task_name_id            INT          NOT NULL REFERENCES tax_task_names(id),
    year                    INT          NOT NULL,
    period                  VARCHAR(50)  NOT NULL CHECK (period IN ('JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC','Q1','Q2','Q3','Q4','ANNUALLY')),
    description             TEXT,
    deadline                TIMESTAMPTZ  NOT NULL,
    status                  VARCHAR(50)  NOT NULL CHECK (status IN ('OPEN', 'SUBMITTED', 'REJECTED', 'APPROVED_FOR_FILING', 'FILED', 'COMPLETED')),
    working_files           JSONB        NOT NULL DEFAULT '[]',
    output_file_id          UUID REFERENCES files(id),
    proof_of_filing_file_id UUID REFERENCES files(id),
    created_by              UUID         NOT NULL REFERENCES users(id),
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_trt_client_id              ON tax_record_tasks(client_id);
CREATE INDEX idx_trt_status                 ON tax_record_tasks(status);
CREATE INDEX idx_trt_deadline               ON tax_record_tasks(deadline);
CREATE INDEX idx_trt_created_by             ON tax_record_tasks(created_by);
CREATE INDEX idx_trt_created_at             ON tax_record_tasks(created_at);
CREATE INDEX idx_trt_status_deadline        ON tax_record_tasks(status, deadline);
CREATE INDEX idx_trt_client_status          ON tax_record_tasks(client_id, status);
CREATE INDEX idx_trt_client_status_deadline ON tax_record_tasks(client_id, status, deadline);

-- Tax record task accountants join table
CREATE TABLE tax_record_task_accountants (
    task_id UUID NOT NULL REFERENCES tax_record_tasks(id),
    user_id UUID NOT NULL REFERENCES users(id),
    PRIMARY KEY (task_id, user_id)
);

CREATE INDEX idx_task_accountants_task_id ON tax_record_task_accountants(task_id);
CREATE INDEX idx_task_accountants_user_id ON tax_record_task_accountants(user_id);

-- Tax record task logs
CREATE TABLE tax_record_task_logs (
    id           UUID PRIMARY KEY,
    task_id      UUID         NOT NULL REFERENCES tax_record_tasks(id),
    action       VARCHAR(50)  NOT NULL CHECK (action IN ('CREATED', 'SUBMITTED', 'RECALLED', 'APPROVED', 'REJECTED', 'APPROVED_FOR_FILING', 'FILED', 'COMPLETED')),
    comment      JSONB,
    performed_by UUID         NOT NULL REFERENCES users(id),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_trt_logs_task_id              ON tax_record_task_logs(task_id);
CREATE INDEX idx_trt_logs_performed_by_created ON tax_record_task_logs(performed_by, created_at);
CREATE INDEX idx_trt_logs_action_created       ON tax_record_task_logs(action, created_at);
