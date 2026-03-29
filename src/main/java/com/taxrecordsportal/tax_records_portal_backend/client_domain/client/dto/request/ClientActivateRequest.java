package com.taxrecordsportal.tax_records_portal_backend.client_domain.client.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientActivateRequest(

        @NotBlank(message = "First name is required.")
        @Size(max = 100)
        String firstName,

        @NotBlank(message = "Last name is required.")
        @Size(max = 100)
        String lastName,

        @NotBlank(message = "Email is required.")
        @Email(message = "Invalid email.")
        @Size(max = 255)
        String email
) {
}
