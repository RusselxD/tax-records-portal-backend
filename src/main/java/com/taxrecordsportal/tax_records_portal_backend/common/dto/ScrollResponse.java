package com.taxrecordsportal.tax_records_portal_backend.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record ScrollResponse<T>(
        List<T> content,
        boolean hasMore
) {
    public static <T> ScrollResponse<T> from(Page<T> page) {
        return new ScrollResponse<>(page.getContent(), page.hasNext());
    }
}
