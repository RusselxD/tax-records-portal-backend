package com.taxrecordsportal.tax_records_portal_backend.consultation_domain.consultation_log;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ConsultationLogSpecification {

    private ConsultationLogSpecification() {}

    public static Specification<ConsultationLog> withFilters(
            UUID clientId,
            ConsultationLogStatus status,
            ConsultationBillableType billableType,
            LocalDate dateFrom,
            LocalDate dateTo,
            String search,
            UUID createdById,
            UUID scopedUserId,
            UUID clientScopedUserId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (clientId != null) {
                predicates.add(cb.equal(root.get("client").get("id"), clientId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (billableType != null) {
                predicates.add(cb.equal(root.get("billableType"), billableType));
            }

            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), dateFrom));
            }

            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), dateTo));
            }

            if (createdById != null) {
                predicates.add(cb.equal(root.get("createdBy").get("id"), createdById));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate subjectMatch = cb.like(cb.lower(root.get("subject")), pattern);
                Predicate platformMatch = cb.like(cb.lower(root.get("platform")), pattern);
                predicates.add(cb.or(subjectMatch, platformMatch));
            }

            // Implicit scoping — OOS/CSD only sees logs they created
            if (scopedUserId != null) {
                predicates.add(cb.equal(root.get("createdBy").get("id"), scopedUserId));
            }

            // Implicit scoping — QTD only sees logs for clients assigned to them
            if (clientScopedUserId != null) {
                Subquery<UUID> clientSub = query.subquery(UUID.class);
                var clientRoot = clientSub.from(Client.class);
                Join<Client, User> clientAccJoin = clientRoot.join("accountants");
                clientSub.select(clientRoot.get("id"));
                clientSub.where(
                        cb.equal(clientRoot.get("id"), root.get("client").get("id")),
                        cb.equal(clientAccJoin.get("id"), clientScopedUserId)
                );
                predicates.add(cb.exists(clientSub));
            }

            query.orderBy(cb.desc(root.get("createdAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
