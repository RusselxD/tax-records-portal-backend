package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task;

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

public final class ClientInfoTaskSpecification {

    private ClientInfoTaskSpecification() {}

    public static Specification<ClientInfoTask> withFilters(
            String search,
            ClientInfoTaskType type,
            ClientInfoTaskStatus status,
            UUID scopedAccountantId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // Scoping: non-viewAll users only see tasks for clients they're assigned to
            if (scopedAccountantId != null) {
                Subquery<UUID> clientSub = query.subquery(UUID.class);
                Root<Client> clientRoot = clientSub.from(Client.class);
                Join<Client, User> accJoin = clientRoot.join("accountants");
                clientSub.select(clientRoot.get("id"));
                clientSub.where(
                        cb.equal(clientRoot.get("id"), root.get("client").get("id")),
                        cb.equal(accJoin.get("id"), scopedAccountantId)
                );
                predicates.add(cb.exists(clientSub));
            }

            // Search: client name (from JSONB snapshot) or submitter name
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                Predicate clientNameMatch = cb.like(
                        cb.lower(cb.concat(root.get("clientInformation").as(String.class), "")),
                        pattern);
                Predicate submitterFirstName = cb.like(
                        cb.lower(root.get("submittedBy").get("firstName")), pattern);
                Predicate submitterLastName = cb.like(
                        cb.lower(root.get("submittedBy").get("lastName")), pattern);
                predicates.add(cb.or(clientNameMatch, submitterFirstName, submitterLastName));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
