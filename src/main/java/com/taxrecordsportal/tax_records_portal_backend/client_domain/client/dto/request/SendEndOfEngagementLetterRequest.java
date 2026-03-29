package com.taxrecordsportal.tax_records_portal_backend.client_domain.client.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SendEndOfEngagementLetterRequest(@NotNull UUID templateId) {}
