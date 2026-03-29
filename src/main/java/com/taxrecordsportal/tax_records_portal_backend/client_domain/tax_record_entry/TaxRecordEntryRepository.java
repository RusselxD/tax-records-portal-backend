package com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.Period;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaxRecordEntryRepository extends JpaRepository<TaxRecordEntry, UUID> {

    @EntityGraph(attributePaths = {"category", "subCategory", "taskName", "outputFile", "proofOfFilingFile"})
    List<TaxRecordEntry> findByClientId(UUID clientId);

    Optional<TaxRecordEntry> findByClientIdAndCategoryIdAndSubCategoryIdAndTaskNameIdAndYearAndPeriod(
            UUID clientId, Integer categoryId, Integer subCategoryId, Integer taskNameId, int year, Period period);

    @Query(nativeQuery = true, value = """
            SELECT CAST(e.category_id AS TEXT) AS id, c.name AS label, COUNT(*) AS count
            FROM tax_record_entries e
            JOIN tax_task_categories c ON c.id = e.category_id
            WHERE e.client_id = :clientId
            GROUP BY e.category_id, c.name
            ORDER BY c.name
            """)
    List<DrillDownProjection> drillDownCategories(@Param("clientId") UUID clientId);

    @Query(nativeQuery = true, value = """
            SELECT CAST(e.sub_category_id AS TEXT) AS id, sc.name AS label, COUNT(*) AS count
            FROM tax_record_entries e
            JOIN tax_task_sub_categories sc ON sc.id = e.sub_category_id
            WHERE e.client_id = :clientId AND e.category_id = :categoryId
            GROUP BY e.sub_category_id, sc.name
            ORDER BY sc.name
            """)
    List<DrillDownProjection> drillDownSubCategories(@Param("clientId") UUID clientId,
                                                     @Param("categoryId") Integer categoryId);

    @Query(nativeQuery = true, value = """
            SELECT CAST(e.task_name_id AS TEXT) AS id, tn.name AS label, COUNT(*) AS count
            FROM tax_record_entries e
            JOIN tax_task_names tn ON tn.id = e.task_name_id
            WHERE e.client_id = :clientId AND e.sub_category_id = :subCategoryId
            GROUP BY e.task_name_id, tn.name
            ORDER BY tn.name
            """)
    List<DrillDownProjection> drillDownTaskNames(@Param("clientId") UUID clientId,
                                                  @Param("subCategoryId") Integer subCategoryId);

    @Query(nativeQuery = true, value = """
            SELECT CAST(e.year AS TEXT) AS id, CAST(e.year AS TEXT) AS label, COUNT(*) AS count
            FROM tax_record_entries e
            WHERE e.client_id = :clientId AND e.task_name_id = :taskNameId
            GROUP BY e.year
            ORDER BY e.year DESC
            """)
    List<DrillDownProjection> drillDownYears(@Param("clientId") UUID clientId,
                                              @Param("taskNameId") Integer taskNameId);

    @Query(nativeQuery = true, value = """
            SELECT e.period AS id, e.period AS label, COUNT(*) AS count
            FROM tax_record_entries e
            WHERE e.client_id = :clientId AND e.task_name_id = :taskNameId AND e.year = :year
            GROUP BY e.period
            ORDER BY e.period
            """)
    List<DrillDownProjection> drillDownPeriods(@Param("clientId") UUID clientId,
                                                @Param("taskNameId") Integer taskNameId,
                                                @Param("year") int year);

    @EntityGraph(attributePaths = {"category", "subCategory", "taskName", "outputFile", "proofOfFilingFile"})
    @Query("SELECT e FROM TaxRecordEntry e WHERE e.client.id = :clientId AND e.category.id = :categoryId " +
            "AND e.subCategory.id = :subCategoryId AND e.taskName.id = :taskNameId AND e.year = :year AND e.period = :period")
    Optional<TaxRecordEntry> findRecordWithFiles(@Param("clientId") UUID clientId,
                                                  @Param("categoryId") Integer categoryId,
                                                  @Param("subCategoryId") Integer subCategoryId,
                                                  @Param("taskNameId") Integer taskNameId,
                                                  @Param("year") int year,
                                                  @Param("period") Period period);

    @EntityGraph(attributePaths = {"category", "subCategory", "taskName"})
    @Query("SELECT e FROM TaxRecordEntry e WHERE e.client.id = :clientId AND e.createdAt >= :since ORDER BY e.createdAt DESC LIMIT 5")
    List<TaxRecordEntry> findRecentByClientId(@Param("clientId") UUID clientId, @Param("since") Instant since);

    boolean existsByCategoryId(Integer categoryId);

    boolean existsBySubCategoryId(Integer subCategoryId);

    boolean existsByTaskNameId(Integer taskNameId);
}
