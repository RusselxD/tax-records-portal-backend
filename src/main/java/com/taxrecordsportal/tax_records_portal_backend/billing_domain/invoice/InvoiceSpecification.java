package com.taxrecordsportal.tax_records_portal_backend.billing_domain.invoice;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class InvoiceSpecification {

    private InvoiceSpecification() {}

    private static String escapeLike(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public static Specification<Invoice> withFilters(UUID clientId, InvoiceStatus status, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (clientId != null) {
                predicates.add(cb.equal(root.get("client").get("id"), clientId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + escapeLike(search.trim().toLowerCase()) + "%";
                Predicate invoiceNumberMatch = cb.like(cb.lower(root.get("invoiceNumber")), pattern);
                Predicate clientNameMatch = cb.like(
                        cb.lower(cb.concat(root.get("client").get("clientInfo").get("clientInformation").as(String.class), "")),
                        pattern);
                predicates.add(cb.or(invoiceNumberMatch, clientNameMatch));
            }

            query.orderBy(cb.desc(root.get("createdAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
