package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto;

import java.util.UUID;

public interface ClientDisplayNameProjection {
    UUID getClientId();
    String getRegisteredName();
    String getTradeName();
}
