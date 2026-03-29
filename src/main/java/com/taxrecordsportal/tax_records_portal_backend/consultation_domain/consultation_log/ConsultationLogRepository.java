package com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsultationLogRepository extends JpaRepository<ConsultationLog, UUID>, JpaSpecificationExecutor<ConsultationLog> {

    @Override
    @EntityGraph(attributePaths = {"client", "client.clientInfo", "createdBy"})
    Page<ConsultationLog> findAll(@NonNull Specification<ConsultationLog> spec, @NonNull Pageable pageable);

    @EntityGraph(attributePaths = {"client", "createdBy"})
    Optional<ConsultationLog> findWithDetailsById(UUID id);

    @EntityGraph(attributePaths = {"client", "client.clientInfo", "client.accountants", "client.accountants.role", "createdBy"})
    Optional<ConsultationLog> findWithFullDetailsById(UUID id);

    List<ConsultationLog> findByClientIdAndDateBetweenAndStatusOrderByDateAscStartTimeAsc(
            UUID clientId, LocalDate start, LocalDate end, ConsultationLogStatus status);

    @Query("SELECT COALESCE(SUM(cl.hours), 0) FROM ConsultationLog cl " +
            "WHERE cl.client.id = :clientId AND cl.date BETWEEN :start AND :end " +
            "AND cl.status = :status")
    BigDecimal sumHoursByClientAndMonth(@Param("clientId") UUID clientId,
                                        @Param("start") LocalDate start,
                                        @Param("end") LocalDate end,
                                        @Param("status") ConsultationLogStatus status);

    @Query("SELECT COALESCE(SUM(cl.hours), 0) FROM ConsultationLog cl " +
            "WHERE cl.client.id = :clientId AND cl.date BETWEEN :start AND :end " +
            "AND cl.status = :status AND cl.billableType = :billableType")
    BigDecimal sumHoursByClientAndMonthAndBillableType(@Param("clientId") UUID clientId,
                                                       @Param("start") LocalDate start,
                                                       @Param("end") LocalDate end,
                                                       @Param("status") ConsultationLogStatus status,
                                                       @Param("billableType") ConsultationBillableType billableType);
}
