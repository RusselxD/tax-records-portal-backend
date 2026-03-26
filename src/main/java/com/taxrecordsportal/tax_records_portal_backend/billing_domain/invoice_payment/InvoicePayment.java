package com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice_payment;

import com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice.Invoice;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.FileReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "invoice_payments", indexes = {
        @Index(name = "idx_invoice_payments_invoice_id", columnList = "invoice_id")
})
public class InvoicePayment {

    @Id
    @GeneratedValue(strategy = UUID)
    @EqualsAndHashCode.Include
    private java.util.UUID id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "attachments", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<FileReference> attachments;

    @Column(name = "email_sent", nullable = false)
    private boolean emailSent = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
