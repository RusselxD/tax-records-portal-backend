package com.taxrecordsportal.tax_records_portal_backend.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public final class DateUtil {

    public static final ZoneId ZONE_PH = ZoneId.of("Asia/Manila");

    private DateUtil() {}

    public static Instant toStartOfDay(LocalDate date) {
        return date.atStartOfDay(ZONE_PH).toInstant();
    }
}
