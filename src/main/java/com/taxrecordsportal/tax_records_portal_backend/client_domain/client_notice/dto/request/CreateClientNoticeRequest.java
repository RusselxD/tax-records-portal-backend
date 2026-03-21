package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_notice.dto.request;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_notice.NoticeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateClientNoticeRequest(
        @NotNull NoticeType type,
        @NotBlank String content
) {}
