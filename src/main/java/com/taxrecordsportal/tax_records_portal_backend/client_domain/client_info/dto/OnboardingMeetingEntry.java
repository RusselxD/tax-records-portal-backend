package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto;

import java.util.Map;

public record OnboardingMeetingEntry(
        String titleOfMeeting,
        DateField date,
        String timeStarted,
        String timeEnded,
        String agenda,
        LinkReference linkToMeetingRecording,
        Map<String, Object> minutes
) {
}
