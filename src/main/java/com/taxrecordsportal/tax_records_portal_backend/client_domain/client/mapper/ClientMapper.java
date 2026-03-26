package com.taxrecordsportal.tax_records_portal_backend.client_domain.client.mapper;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.dto.response.OnboardingClientListItemResponse;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.ClientInfo;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.ClientInformation;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.CorporateOfficerInformation;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.PointOfContactDetails;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_template.ClientInfoTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    @Mapping(target = "name", expression = "java(computeClientName(client))")
    @Mapping(target = "email", expression = "java(computeClientEmail(client))")
    @Mapping(target = "hasActiveTask", ignore = true)
    @Mapping(target = "activeTaskId", ignore = true)
    @Mapping(target = "lastTaskId", ignore = true)
    @Mapping(target = "handedOff", source = "handedOff")
    OnboardingClientListItemResponse toOnboardingListItem(Client client);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "client", ignore = true)
    ClientInfo fromTemplate(ClientInfoTemplate template);

    default String computeClientName(Client client) {
        ClientInfo info = client.getClientInfo();
        if (info == null) return null;

        ClientInformation ci = info.getClientInformation();
        if (ci == null) return null;

        String registered = ci.registeredName();
        String trade = ci.tradeName();

        if (registered != null && trade != null) {
            return registered + " (" + trade + ")";
        }
        if (registered != null) return registered;
        return trade;
    }

    default String computeClientEmail(Client client) {
        if (client.getUsers() != null && !client.getUsers().isEmpty()) {
            String email = client.getUsers().iterator().next().getEmail();
            if (email != null) return email;
        }

        ClientInfo info = client.getClientInfo();
        if (info == null) return null;

        CorporateOfficerInformation coi = info.getCorporateOfficerInformation();
        if (coi == null) return null;

        PointOfContactDetails poc = coi.pointOfContact();
        if (poc == null) return null;

        return poc.emailAddress();
    }
}
