package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.dto;

public record OnTimeRateResponse(
        int totalCompleted,
        int completedOnTime,
        int completedLate,
        double onTimeRate
) {}
