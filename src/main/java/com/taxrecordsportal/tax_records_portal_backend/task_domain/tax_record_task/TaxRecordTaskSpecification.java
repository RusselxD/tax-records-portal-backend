package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TaxRecordTaskSpecification {

    private TaxRecordTaskSpecification() {}

    /**
     * Builds a dynamic WHERE clause from all non-null params.
     *
     * @param accountantId       explicit filter — "show tasks assigned to this accountant" (Manager/QTD only)
     * @param scopedUserId       implicit scoping — when non-null, restricts results to tasks assigned to this user (CSD/OOS)
     * @param clientScopedUserId implicit scoping — when non-null, restricts results to tasks for clients assigned to this user (QTD)
     */
    public static Specification<TaxRecordTask> withFilters(
            String search,
            UUID clientId,
            Integer categoryId,
            Integer subCategoryId,
            Integer taskNameId,
            Integer year,
            Period period,
            TaxRecordTaskStatus status,
            UUID accountantId,
            UUID scopedUserId,
            UUID clientScopedUserId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // --- Exact-match filters (all optional) ---
            if (clientId != null) {
                predicates.add(cb.equal(root.get("client").get("id"), clientId));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (subCategoryId != null) {
                predicates.add(cb.equal(root.get("subCategory").get("id"), subCategoryId));
            }
            if (taskNameId != null) {
                predicates.add(cb.equal(root.get("taskName").get("id"), taskNameId));
            }
            if (year != null) {
                predicates.add(cb.equal(root.get("year"), year));
            }
            if (period != null) {
                predicates.add(cb.equal(root.get("period"), period));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // --- Collection filters (use EXISTS subquery to avoid cartesian products) ---

            // Explicit accountant filter (Manager/QTD picking a specific accountant)
            if (accountantId != null) {
                Subquery<UUID> accSub = query.subquery(UUID.class);
                var accRoot = accSub.correlate(root);
                Join<TaxRecordTask, User> accJoin = accRoot.join("assignedTo");
                accSub.select(accJoin.get("id"));
                accSub.where(cb.equal(accJoin.get("id"), accountantId));
                predicates.add(cb.exists(accSub));
            }

            // Implicit scoping — CSD/OOS without task.view.all only sees their own assigned tasks
            if (scopedUserId != null) {
                Subquery<UUID> scopeSub = query.subquery(UUID.class);
                var scopeRoot = scopeSub.correlate(root);
                Join<TaxRecordTask, User> scopeJoin = scopeRoot.join("assignedTo");
                scopeSub.select(scopeJoin.get("id"));
                scopeSub.where(cb.equal(scopeJoin.get("id"), scopedUserId));
                predicates.add(cb.exists(scopeSub));
            }

            // Implicit scoping — QTD only sees tasks for clients assigned to them
            if (clientScopedUserId != null) {
                Subquery<UUID> clientSub = query.subquery(UUID.class);
                Root<Client> clientRoot = clientSub.from(Client.class);
                Join<Client, User> clientAccJoin = clientRoot.join("accountants");
                clientSub.select(clientRoot.get("id"));
                clientSub.where(
                        cb.equal(clientRoot.get("id"), root.get("client").get("id")),
                        cb.equal(clientAccJoin.get("id"), clientScopedUserId)
                );
                predicates.add(cb.exists(clientSub));
            }

            // --- Text search (OR across task name and client name inside JSONB) ---
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                Predicate taskNameMatch = cb.like(
                        cb.lower(root.get("taskName").get("name")), pattern);
                // JSONB → text: concat with empty string forces Postgres text coercion
                Predicate clientNameMatch = cb.like(
                        cb.lower(cb.concat(root.get("client").get("clientInfo").get("clientInformation").as(String.class), "")),
                        pattern);
                predicates.add(cb.or(taskNameMatch, clientNameMatch));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
