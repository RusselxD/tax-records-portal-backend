package com.taxrecordsportal.tax_records_portal_backend.user_domain.role;

import lombok.Getter;

@Getter
public enum RoleKey {
    MANAGER("Manager"),
    OOS("Onboarding, Offboarding & Support"),
    QTD("Quality, Training & Development"),
    CSD("Client Service Delivery"),
    BILLING("Internal Accounting / Billing"),
    CLIENT("Client");

    private final String displayName;

    RoleKey(String displayName) {
        this.displayName = displayName;
    }

}
