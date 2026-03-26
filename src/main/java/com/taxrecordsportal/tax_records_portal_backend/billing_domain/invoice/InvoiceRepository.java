package com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice;

import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.dto.response.BillingClientProjection;
import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.dto.response.ClientNameProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    boolean existsByInvoiceNumber(String invoiceNumber);

    @EntityGraph(attributePaths = {"terms"})
    Page<Invoice> findByClientIdOrderByCreatedAtDesc(UUID clientId, Pageable pageable);

    @EntityGraph(attributePaths = {"terms"})
    Page<Invoice> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"terms", "payments"})
    Optional<Invoice> findWithDetailsById(UUID id);

    @EntityGraph(attributePaths = {"payments"})
    List<Invoice> findByClientIdAndStatusInOrderByDueDateAsc(UUID clientId, List<InvoiceStatus> statuses);


    @Query(nativeQuery = true, value = """
            SELECT ci.client_id AS clientId,
                   ci.client_information ->> 'registeredName' AS registeredName,
                   ci.client_information ->> 'tradeName' AS tradeName
            FROM client_info ci
            WHERE ci.client_id IN (:clientIds)
            """)
    List<ClientNameProjection> findClientNamesByIds(@Param("clientIds") List<UUID> clientIds);

    @Query(nativeQuery = true, value = """
            SELECT
                c.id AS clientId,
                ci.client_information ->> 'registeredName' AS registeredName,
                ci.client_information ->> 'tradeName' AS tradeName,
                COUNT(i.id) AS totalInvoices,
                COUNT(i.id) FILTER (WHERE i.status = 'UNPAID') AS unpaidInvoices,
                COUNT(i.id) FILTER (WHERE i.status = 'PARTIALLY_PAID') AS partiallyPaidInvoices,
                COUNT(i.id) FILTER (WHERE i.status = 'FULLY_PAID') AS fullyPaidInvoices,
                COALESCE(SUM(i.amount_due) FILTER (WHERE i.voided = FALSE), 0) AS totalAmountDue,
                COALESCE(SUM(i.amount_due) FILTER (WHERE i.voided = FALSE), 0)
                  - COALESCE(SUM(p.paid) FILTER (WHERE i.voided = FALSE), 0) AS totalBalance
            FROM clients c
            JOIN client_info ci ON ci.client_id = c.id
            LEFT JOIN invoices i ON i.client_id = c.id
            LEFT JOIN (
                SELECT invoice_id, SUM(amount) AS paid
                FROM invoice_payments
                GROUP BY invoice_id
            ) p ON p.invoice_id = i.id
            WHERE (:search IS NULL OR ci.client_information ->> 'registeredName' ILIKE CONCAT('%', :search, '%')
                   OR ci.client_information ->> 'tradeName' ILIKE CONCAT('%', :search, '%'))
            GROUP BY c.id, ci.client_information
            ORDER BY COUNT(i.id) DESC, ci.client_information ->> 'registeredName' ASC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM clients c
            JOIN client_info ci ON ci.client_id = c.id
            WHERE (:search IS NULL OR ci.client_information ->> 'registeredName' ILIKE CONCAT('%', :search, '%')
                   OR ci.client_information ->> 'tradeName' ILIKE CONCAT('%', :search, '%'))
            """)
    Page<BillingClientProjection> findBillingClientSummaries(@Param("search") String search, Pageable pageable);

    @Query(nativeQuery = true, value = """
            SELECT DISTINCT c.id
            FROM clients c
            JOIN users u ON u.client_id = c.id
            WHERE c.id IN (:clientIds)
              AND u.email IS NOT NULL
            """)
    List<UUID> findClientIdsWithEmail(@Param("clientIds") List<UUID> clientIds);
}
