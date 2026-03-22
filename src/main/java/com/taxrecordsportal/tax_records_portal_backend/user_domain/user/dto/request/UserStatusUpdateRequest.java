package com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.request;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(
        @NotNull(message = "Status is required.")
        UserStatus status
) {}
