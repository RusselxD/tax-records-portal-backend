package com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record ResendActivationRequest(

        @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters.")
        String firstName,

        @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters.")
        String lastName,

        @Email(message = "Invalid email.")
        String email

) { }
