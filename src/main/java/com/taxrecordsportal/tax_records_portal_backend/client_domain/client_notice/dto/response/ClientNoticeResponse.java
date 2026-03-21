package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_notice.dto.response;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_notice.NoticeType;

public record ClientNoticeResponse(
        Integer id,
        NoticeType type,
        String content
) {}
