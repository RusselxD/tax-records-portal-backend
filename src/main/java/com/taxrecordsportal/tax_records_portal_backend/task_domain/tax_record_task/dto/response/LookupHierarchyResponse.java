package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response;

import java.util.List;

public record LookupHierarchyResponse(
        Integer id,
        String name,
        List<SubCategoryItem> subCategories
) {
    public record SubCategoryItem(
            Integer id,
            String name,
            List<TaskNameItem> taskNames
    ) {}

    public record TaskNameItem(
            Integer id,
            String name
    ) {}
}
