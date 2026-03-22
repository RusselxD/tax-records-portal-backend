-- Files
CREATE TABLE files (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    url         VARCHAR(255) NOT NULL,
    client_id   UUID         NOT NULL REFERENCES clients(id),
    uploaded_by UUID         NOT NULL REFERENCES users(id),
    uploaded_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_files_client_id ON files(client_id);

-- Add file FKs to tax_record_entries (created in V2)
ALTER TABLE tax_record_entries
    ADD CONSTRAINT fk_tre_output_file FOREIGN KEY (output_file_id) REFERENCES files(id),
    ADD CONSTRAINT fk_tre_proof_of_filing_file FOREIGN KEY (proof_of_filing_file_id) REFERENCES files(id);
