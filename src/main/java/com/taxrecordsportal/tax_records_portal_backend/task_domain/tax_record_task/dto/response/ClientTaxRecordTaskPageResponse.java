package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response;

import java.util.List;

public record ClientTaxRecordTaskPageResponse(
        List<ClientTaxRecordTaskItem> tasks,
        String nextCursor,
        boolean hasMore
) {}
