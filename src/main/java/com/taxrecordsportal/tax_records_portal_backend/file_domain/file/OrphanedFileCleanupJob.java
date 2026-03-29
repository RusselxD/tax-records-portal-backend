package com.taxrecordsportal.tax_records_portal_backend.file_domain.file;

import com.taxrecordsportal.tax_records_portal_backend.common.R2StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanedFileCleanupJob {

    private final JdbcTemplate jdbcTemplate;
    private final FileRepository fileRepository;
    private final R2StorageService r2StorageService;
    private final TransactionTemplate transactionTemplate;

    private static final String REFERENCED_FILE_IDS_QUERY = """
            -- 1. Direct FK columns on tax_record_tasks
            SELECT output_file_id AS file_id FROM tax_record_tasks WHERE output_file_id IS NOT NULL
            UNION
            SELECT proof_of_filing_file_id FROM tax_record_tasks WHERE proof_of_filing_file_id IS NOT NULL
            UNION
            -- 2. Direct FK columns on tax_record_entries
            SELECT output_file_id FROM tax_record_entries WHERE output_file_id IS NOT NULL
            UNION
            SELECT proof_of_filing_file_id FROM tax_record_entries WHERE proof_of_filing_file_id IS NOT NULL
            UNION
            -- 3. JSONB working_files on tax_record_tasks
            SELECT (jsonb_array_elements(working_files) ->> 'fileId')::uuid
            FROM tax_record_tasks WHERE working_files IS NOT NULL AND jsonb_array_length(working_files) > 0
            UNION
            -- 4. JSONB working_files on tax_record_entries
            SELECT (jsonb_array_elements(working_files) ->> 'fileId')::uuid
            FROM tax_record_entries WHERE working_files IS NOT NULL AND jsonb_array_length(working_files) > 0
            UNION
            -- 5. JSONB attachments on invoices
            SELECT (jsonb_array_elements(attachments) ->> 'id')::uuid
            FROM invoices WHERE attachments IS NOT NULL AND jsonb_array_length(attachments) > 0
            UNION
            -- 6. JSONB attachments on invoice_payments
            SELECT (jsonb_array_elements(attachments) ->> 'id')::uuid
            FROM invoice_payments WHERE attachments IS NOT NULL AND jsonb_array_length(attachments) > 0
            UNION
            -- 7. JSONB attachments on consultation_logs
            SELECT (jsonb_array_elements(attachments) ->> 'id')::uuid
            FROM consultation_logs WHERE attachments IS NOT NULL AND jsonb_array_length(attachments) > 0
            UNION
            -- 8. Engagement letters in client_info scope_of_engagement
            SELECT (jsonb_array_elements(scope_of_engagement -> 'engagementLetters') ->> 'id')::uuid
            FROM client_info WHERE scope_of_engagement -> 'engagementLetters' IS NOT NULL
              AND jsonb_array_length(COALESCE(scope_of_engagement -> 'engagementLetters', '[]'::jsonb)) > 0
            UNION
            -- 9. Looseleaf certificates in client_info scope_of_engagement
            SELECT (jsonb_array_elements(scope_of_engagement -> 'looseleafCertificateAndBirTemplates') ->> 'id')::uuid
            FROM client_info WHERE scope_of_engagement -> 'looseleafCertificateAndBirTemplates' IS NOT NULL
              AND jsonb_array_length(COALESCE(scope_of_engagement -> 'looseleafCertificateAndBirTemplates', '[]'::jsonb)) > 0
            UNION
            -- 10. All FileReference 'id' fields nested in client_information JSONB (recursive extraction)
            SELECT (val ->> 'id')::uuid
            FROM client_info, LATERAL jsonb_path_query(client_information, 'strict $.**.id') AS val
            WHERE client_information IS NOT NULL AND val IS NOT NULL AND (val ->> 'id') IS NOT NULL
              AND (val ->> 'id') ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
            UNION
            -- 11. Corporate officer ID scanned files
            SELECT (officer ->> 'id')::uuid
            FROM client_info,
                 LATERAL jsonb_path_query(corporate_officer_information, 'strict $.**.idScannedWith3Signature') AS officer
            WHERE corporate_officer_information IS NOT NULL AND officer IS NOT NULL AND (officer ->> 'id') IS NOT NULL
            UNION
            -- 12. Same for client_info_tasks
            SELECT (jsonb_array_elements(scope_of_engagement -> 'engagementLetters') ->> 'id')::uuid
            FROM client_info_tasks WHERE scope_of_engagement -> 'engagementLetters' IS NOT NULL
              AND jsonb_array_length(COALESCE(scope_of_engagement -> 'engagementLetters', '[]'::jsonb)) > 0
            UNION
            SELECT (jsonb_array_elements(scope_of_engagement -> 'looseleafCertificateAndBirTemplates') ->> 'id')::uuid
            FROM client_info_tasks WHERE scope_of_engagement -> 'looseleafCertificateAndBirTemplates' IS NOT NULL
              AND jsonb_array_length(COALESCE(scope_of_engagement -> 'looseleafCertificateAndBirTemplates', '[]'::jsonb)) > 0
            UNION
            SELECT (val ->> 'id')::uuid
            FROM client_info_tasks, LATERAL jsonb_path_query(client_information, 'strict $.**.id') AS val
            WHERE client_information IS NOT NULL AND val IS NOT NULL AND (val ->> 'id') IS NOT NULL
              AND (val ->> 'id') ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
            UNION
            -- 13. TipTap image references in rich text (extract UUID from /api/v1/files/images/{uuid})
            SELECT (regexp_matches(comment::text, '/api/v1/files/images/([0-9a-f-]{36})', 'g'))[1]::uuid
            FROM tax_record_task_logs WHERE comment IS NOT NULL AND comment::text LIKE '%/api/v1/files/images/%'
            UNION
            SELECT (regexp_matches(comment::text, '/api/v1/files/images/([0-9a-f-]{36})', 'g'))[1]::uuid
            FROM client_info_task_logs WHERE comment IS NOT NULL AND comment::text LIKE '%/api/v1/files/images/%'
            UNION
            SELECT (regexp_matches(comment::text, '/api/v1/files/images/([0-9a-f-]{36})', 'g'))[1]::uuid
            FROM consultation_log_audits WHERE comment IS NOT NULL AND comment::text LIKE '%/api/v1/files/images/%'
            UNION
            SELECT (regexp_matches(notes::text, '/api/v1/files/images/([0-9a-f-]{36})', 'g'))[1]::uuid
            FROM consultation_logs WHERE notes IS NOT NULL AND notes::text LIKE '%/api/v1/files/images/%'
            UNION
            SELECT (regexp_matches(body::text, '/api/v1/files/images/([0-9a-f-]{36})', 'g'))[1]::uuid
            FROM end_of_engagement_letter_templates WHERE body IS NOT NULL AND body::text LIKE '%/api/v1/files/images/%'
            """;

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Manila")
    public void cleanupOrphanedFiles() {
        List<FileEntity> orphaned = transactionTemplate.execute(status -> findOrphanedFiles());

        if (orphaned == null || orphaned.isEmpty()) {
            log.debug("No orphaned files found");
            return;
        }

        int deleted = 0;
        for (FileEntity file : orphaned) {
            try {
                r2StorageService.delete(file.getUrl());
                transactionTemplate.executeWithoutResult(status -> fileRepository.delete(file));
                deleted++;
            } catch (Exception e) {
                log.warn("Failed to delete orphaned file {}: {}", file.getId(), e.getMessage());
            }
        }

        log.info("Orphaned file cleanup: deleted {}/{} files", deleted, orphaned.size());
    }

    private List<FileEntity> findOrphanedFiles() {
        String query = """
                SELECT f.id FROM files f
                WHERE f.id NOT IN (%s)
                  AND f.uploaded_at < NOW() - INTERVAL '1 hour'
                """.formatted(REFERENCED_FILE_IDS_QUERY);

        List<UUID> orphanedIds = jdbcTemplate.queryForList(query, UUID.class);

        if (orphanedIds.isEmpty()) return List.of();
        return fileRepository.findAllById(orphanedIds);
    }
}
