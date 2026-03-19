package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto;

public record PointOfContactDetails(
        String contactPerson,
        String contactNumber,
        String deliveryAddress,
        String landmarkPinLocation,
        String emailAddress,
        String preferredMethodOfCommunication,
        String alternativeContact
) {
}
