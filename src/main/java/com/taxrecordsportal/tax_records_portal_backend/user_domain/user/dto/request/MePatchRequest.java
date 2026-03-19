package com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.request;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.common.UserTitle;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MePatchRequest(
        @Size(min = 1, message = "First name cannot be blank.")
        String firstName,

        @Size(min = 1, message = "Last name cannot be blank.")
        String lastName,

        @Email(message = "Invalid email.")
        String email,

        List<UserTitle> titles
) {}
