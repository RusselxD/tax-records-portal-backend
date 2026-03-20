package com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.Period;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaxRecordEntryRepository extends JpaRepository<TaxRecordEntry, UUID> {

    @EntityGraph(attributePaths = {"category", "subCategory", "taskName", "outputFile", "proofOfFilingFile"})
    List<TaxRecordEntry> findByClientId(UUID clientId);

    Optional<TaxRecordEntry> findByClientIdAndCategoryIdAndSubCategoryIdAndTaskNameIdAndYearAndPeriod(
            UUID clientId, Integer categoryId, Integer subCategoryId, Integer taskNameId, int year, Period period);
}
