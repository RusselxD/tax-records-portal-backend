package com.taxrecordsportal.tax_records_portal_backend.analytics_domain.dto.system;

import java.util.List;

public record TaskCompletionTrendResponse(List<String> labels, List<Integer> values) {}
