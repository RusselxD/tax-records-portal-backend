package com.taxrecordsportal.tax_records_portal_backend.common.exception;

import lombok.Getter;

@Getter
public class ClientInfoSectionException extends RuntimeException {

    private final String sectionKey;
    private final String fieldPath;
    private final String detail;

    public ClientInfoSectionException(String sectionKey, String fieldPath, String detail) {
        super("Invalid request body for section: " + sectionKey);
        this.sectionKey = sectionKey;
        this.fieldPath = fieldPath;
        this.detail = detail;
    }
}
