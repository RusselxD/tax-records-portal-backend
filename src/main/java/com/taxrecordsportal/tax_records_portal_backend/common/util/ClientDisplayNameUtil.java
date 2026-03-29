package com.taxrecordsportal.tax_records_portal_backend.common.util;

public final class ClientDisplayNameUtil {

    private ClientDisplayNameUtil() {}

    public static String format(String registeredName, String tradeName) {
        if (registeredName != null && tradeName != null) {
            return registeredName + " (" + tradeName + ")";
        }
        if (registeredName != null) return registeredName;
        return tradeName;
    }
}
