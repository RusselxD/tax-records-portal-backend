package com.taxrecordsportal.tax_records_portal_backend.common.util;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import org.hibernate.Hibernate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static com.taxrecordsportal.tax_records_portal_backend.common.util.SecurityUtil.getCurrentUser;

@Component
public class ClientAccessHelper {

    /**
     * Checks if the current user can access the given client's data.
     * Grants access if the user holds any of the specified permissions,
     * or if they are the creator, an assigned accountant, or a client user.
     * Only checks relationships that are already initialized on the client entity
     * to avoid triggering unwanted lazy loads.
     *
     * @param client           the client (must have relevant relationships loaded)
     * @param forbiddenMessage the 403 message if access is denied
     * @param viewPermissions  one or more permissions that grant unrestricted access
     */
    public void enforceAccess(Client client, String forbiddenMessage, String... viewPermissions) {
        User currentUser = getCurrentUser();

        if (hasAnyPermission(currentUser, viewPermissions)) return;

        if (!isRelated(client, currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, forbiddenMessage);
        }
    }

    /**
     * Same as {@link #enforceAccess} but also blocks client users
     * when tax records protection is enabled on the client.
     */
    public void enforceAccessWithProtection(Client client, String forbiddenMessage, String... viewPermissions) {
        User currentUser = getCurrentUser();

        if (hasAnyPermission(currentUser, viewPermissions)) return;

        if (!isRelated(client, currentUser)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, forbiddenMessage);
        }

        if (isClientUser(client, currentUser)
                && client.getOffboarding() != null && client.getOffboarding().isTaxRecordsProtected()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tax records are protected");
        }
    }

    private boolean hasAnyPermission(User user, String... permissions) {
        return user.getAuthorities().stream()
                .anyMatch(a -> {
                    String authority = a.getAuthority();
                    for (String p : permissions) {
                        if (authority.equals(p)) return true;
                    }
                    return false;
                });
    }

    private boolean isRelated(Client client, User currentUser) {
        UUID userId = currentUser.getId();

        if (isInitializedProxy(client.getCreatedBy())
                && client.getCreatedBy().getId().equals(userId)) {
            return true;
        }

        if (Hibernate.isInitialized(client.getAccountants())
                && client.getAccountants().stream().anyMatch(a -> a.getId().equals(userId))) {
            return true;
        }

        if (Hibernate.isInitialized(client.getUsers())
                && client.getUsers().stream().anyMatch(u -> u.getId().equals(userId))) {
            return true;
        }

        return false;
    }

    private boolean isClientUser(Client client, User currentUser) {
        return Hibernate.isInitialized(client.getUsers())
                && client.getUsers().stream().anyMatch(u -> u.getId().equals(currentUser.getId()));
    }

    private boolean isInitializedProxy(Object proxy) {
        return proxy != null && Hibernate.isInitialized(proxy);
    }
}
