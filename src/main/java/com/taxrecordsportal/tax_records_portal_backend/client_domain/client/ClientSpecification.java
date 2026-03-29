package com.taxrecordsportal.tax_records_portal_backend.client_domain.client;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ClientSpecification {

    private ClientSpecification() {}

    private static String escapeLike(String input) {
        return input.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * @param search       partial client name match (JSONB clientInformation cast to text)
     * @param scopedUserId non-null = only clients assigned to this user
     */
    public static Specification<Client> withFilters(String search, UUID scopedUserId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Scoping: only clients assigned to this user
            if (scopedUserId != null) {
                Subquery<UUID> sub = query.subquery(UUID.class);
                var subRoot = sub.correlate(root);
                Join<Client, User> accJoin = subRoot.join("accountants");
                sub.select(accJoin.get("id"));
                sub.where(cb.equal(accJoin.get("id"), scopedUserId));
                predicates.add(cb.exists(sub));
            }

            // Search: client name inside JSONB
            if (search != null && !search.isBlank()) {
                String pattern = "%" + escapeLike(search.toLowerCase()) + "%";
                predicates.add(cb.like(
                        cb.lower(cb.concat(
                                root.get("clientInfo").get("clientInformation").as(String.class), "")),
                        pattern));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
