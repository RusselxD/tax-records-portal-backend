package com.taxrecordsportal.tax_records_portal_backend.common.util;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.common.UserTitle;

import java.util.List;
import java.util.stream.Collectors;

public final class UserDisplayUtil {

    private UserDisplayUtil() {}

    public static String formatDisplayName(User user) {
        if (user == null) return null;

        String fullName = user.getFirstName() + " " + user.getLastName();
        List<UserTitle> titles = user.getTitles();
        if (titles == null || titles.isEmpty()) {
            return fullName;
        }

        String prefixes = titles.stream()
                .filter(UserTitle::prefix)
                .map(UserTitle::title)
                .collect(Collectors.joining(", "));

        String suffixes = titles.stream()
                .filter(t -> !t.prefix())
                .map(UserTitle::title)
                .collect(Collectors.joining(", "));

        StringBuilder sb = new StringBuilder();
        if (!prefixes.isEmpty()) {
            sb.append(prefixes).append(" ");
        }
        sb.append(fullName);
        if (!suffixes.isEmpty()) {
            sb.append(", ").append(suffixes);
        }
        return sb.toString();
    }
}
