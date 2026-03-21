package com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientRepository;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.ClientInfo;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.ClientInfoRepository;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.*;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry.dto.response.ImportantDateResponse;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.taxrecordsportal.tax_records_portal_backend.common.util.SecurityUtil.getCurrentUser;

@Service
@RequiredArgsConstructor
public class ImportantDateService {

    private final ClientRepository clientRepository;
    private final ClientInfoRepository clientInfoRepository;

    @Transactional(readOnly = true)
    public List<ImportantDateResponse> getImportantDates() {
        User currentUser = getCurrentUser();
        UUID clientId = clientRepository.findClientIdByUserId(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        ClientInfo info = clientInfoRepository.findByClientId(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client info not found"));

        List<ImportantDateResponse> dates = new ArrayList<>();

        extractMainDetails(info.getMainDetails(), dates);
        extractClientInformation(info.getClientInformation(), dates);
        extractCorporateOfficerInformation(info.getCorporateOfficerInformation(), dates);
        extractScopeOfEngagement(info.getScopeOfEngagement(), dates);
        extractOnboardingDetails(info.getOnboardingDetails(), dates);

        dates.sort(Comparator.comparing(ImportantDateResponse::date));
        return dates;
    }

    private void addIfImportant(DateField field, String label, List<ImportantDateResponse> dates) {
        if (field != null && field.date() != null && field.isImportant()) {
            dates.add(new ImportantDateResponse(field.date(), label));
        }
    }

    private void extractMainDetails(MainDetails main, List<ImportantDateResponse> dates) {
        if (main == null) return;
        addIfImportant(main.commencementOfWork(), "Commencement of Work", dates);
    }

    private void extractClientInformation(ClientInformation ci, List<ImportantDateResponse> dates) {
        if (ci == null) return;

        // BIR Main Branch
        if (ci.birMainBranch() != null) {
            addIfImportant(ci.birMainBranch().dateOfBirRegistration(), "BIR Registration (Main Branch)", dates);
        }

        // BIR Branches
        if (ci.birBranches() != null) {
            for (BirBranchDetails branch : ci.birBranches()) {
                String name = branch.businessTradeName() != null ? branch.businessTradeName() : "Branch";
                addIfImportant(branch.dateOfBirRegistration(), "BIR Registration (" + name + ")", dates);
            }
        }

        // BIR Tax Compliance
        if (ci.birTaxCompliance() != null) {
            addIfImportant(ci.birTaxCompliance().dateClassifiedTopWithholding(), "Date Classified Top Withholding", dates);
        }

        // DTI
        if (ci.dtiDetails() != null) {
            DtiDetails dti = ci.dtiDetails();
            addIfImportant(dti.dtiDateOfRegistration(), "DTI Date of Registration", dates);
            addIfImportant(dti.dtiDateOfExpiration(), "DTI Date of Expiration", dates);
            addIfImportant(dti.bmbeDateOfRegistration(), "BMBE Date of Registration", dates);
            addIfImportant(dti.bmbeDateOfExpiration(), "BMBE Date of Expiration", dates);
        }

        // SEC
        if (ci.secDetails() != null) {
            addIfImportant(ci.secDetails().dateOfIncorporation(), "SEC Date of Incorporation", dates);
            addIfImportant(ci.secDetails().dateOfActualMeetingPerBylaws(), "SEC Annual Meeting per Bylaws", dates);
        }

        // Government agencies
        if (ci.sssDetails() != null) {
            addIfImportant(ci.sssDetails().dateOfRegistration(), "SSS Date of Registration", dates);
        }
        if (ci.philhealthDetails() != null) {
            addIfImportant(ci.philhealthDetails().dateOfRegistration(), "PhilHealth Date of Registration", dates);
        }
        if (ci.hdmfDetails() != null) {
            addIfImportant(ci.hdmfDetails().dateOfRegistration(), "HDMF Date of Registration", dates);
        }

        // City Hall
        if (ci.cityHallDetails() != null) {
            for (CityHallDetails ch : ci.cityHallDetails()) {
                String city = ch.businessPermitCity() != null ? ch.businessPermitCity() : "City Hall";
                addIfImportant(ch.dateOfRegistration(), "City Hall Registration (" + city + ")", dates);
                addIfImportant(ch.quarterlyDeadlineQ2(), "Quarterly Deadline Q2 (" + city + ")", dates);
                addIfImportant(ch.quarterlyDeadlineQ3(), "Quarterly Deadline Q3 (" + city + ")", dates);
                addIfImportant(ch.quarterlyDeadlineQ4(), "Quarterly Deadline Q4 (" + city + ")", dates);
                addIfImportant(ch.permitExpirationDate(), "Business Permit Expiration (" + city + ")", dates);

                if (ch.firePermit() != null) {
                    addIfImportant(ch.firePermit().expirationDate(), "Fire Permit Expiration (" + city + ")", dates);
                }
                if (ch.sanitaryPermit() != null) {
                    addIfImportant(ch.sanitaryPermit().expirationDate(), "Sanitary Permit Expiration (" + city + ")", dates);
                }
                if (ch.otherPermit() != null) {
                    addIfImportant(ch.otherPermit().expirationDate(), "Other Permit Expiration (" + city + ")", dates);
                }
            }
        }
    }

    private void extractCorporateOfficerInformation(CorporateOfficerInformation coi, List<ImportantDateResponse> dates) {
        if (coi == null || coi.officers() == null) return;
        for (CorporateOfficerDetails officer : coi.officers()) {
            String name = officer.name() != null ? officer.name() : "Officer";
            addIfImportant(officer.birthday(), "Birthday (" + name + ")", dates);
        }
    }

    private void extractScopeOfEngagement(ScopeOfEngagementDetails soe, List<ImportantDateResponse> dates) {
        if (soe == null) return;
        addIfImportant(soe.dateOfEngagementLetter(), "Engagement Letter Date", dates);

        if (soe.consultationHours() != null && soe.consultationHours().consultations() != null) {
            for (ConsultationEntry entry : soe.consultationHours().consultations()) {
                String label = entry.date() != null && entry.date().date() != null
                        ? "Consultation (" + entry.date().date() + ")"
                        : "Consultation";
                addIfImportant(entry.date(), label, dates);
            }
        }
    }

    private void extractOnboardingDetails(OnboardingDetails od, List<ImportantDateResponse> dates) {
        if (od == null) return;
        addIfImportant(od.gcCreatedDate(), "Group Chat Created Date", dates);

        if (od.meetings() != null) {
            for (OnboardingMeetingEntry meeting : od.meetings()) {
                String title = meeting.titleOfMeeting() != null ? meeting.titleOfMeeting() : "Meeting";
                addIfImportant(meeting.date(), "Meeting: " + title, dates);
            }
        }
    }
}
