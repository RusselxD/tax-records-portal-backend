package com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DrillDownResponse(
        String level,
        List<DrillDownItemResponse> items,
        TaxRecordEntryResponse record
) {

    public static DrillDownResponse ofItems(String level, List<DrillDownItemResponse> items) {
        return new DrillDownResponse(level, items, null);
    }

    public static DrillDownResponse ofRecord(TaxRecordEntryResponse record) {
        return new DrillDownResponse("record", null, record);
    }
}
