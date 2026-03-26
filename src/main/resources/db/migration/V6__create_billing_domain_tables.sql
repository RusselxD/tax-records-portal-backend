-- Invoice terms lookup (addable like employee_positions)
CREATE TABLE invoice_terms (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    days INT          NOT NULL
);

-- Invoices
CREATE TABLE invoices (
    id             UUID PRIMARY KEY,
    version        BIGINT        NOT NULL DEFAULT 0,
    client_id      UUID          NOT NULL REFERENCES clients(id),
    invoice_number VARCHAR(255)  NOT NULL UNIQUE,
    invoice_date   DATE          NOT NULL,
    terms_id       INT           NOT NULL REFERENCES invoice_terms(id),
    due_date       DATE          NOT NULL,
    description    TEXT,
    amount_due     DECIMAL(15,2) NOT NULL,
    status         VARCHAR(50)   NOT NULL DEFAULT 'UNPAID',
    voided         BOOLEAN       NOT NULL DEFAULT FALSE,
    email_sent     BOOLEAN       NOT NULL DEFAULT FALSE,
    attachments    JSONB,
    created_by     UUID          NOT NULL REFERENCES users(id),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_invoices_client_id     ON invoices(client_id);
CREATE INDEX idx_invoices_status        ON invoices(status);
CREATE INDEX idx_invoices_due_date      ON invoices(due_date);
CREATE INDEX idx_invoices_client_status ON invoices(client_id, status);

-- Invoice payments
CREATE TABLE invoice_payments (
    id          UUID PRIMARY KEY,
    invoice_id  UUID          NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    date        DATE          NOT NULL,
    amount      DECIMAL(15,2) NOT NULL,
    attachments JSONB,
    email_sent  BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_invoice_payments_invoice_id ON invoice_payments(invoice_id);
