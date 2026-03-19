package com.taxrecordsportal.tax_records_portal_backend.common.util;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

public final class SecurityUtil {

    private SecurityUtil() {}

    public static User getCurrentUser() {
        return (User) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
    }
}
