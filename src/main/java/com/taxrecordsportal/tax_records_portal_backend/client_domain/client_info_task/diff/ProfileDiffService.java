package com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.diff;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.OrganizationType;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.ClientInfo;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info.dto.*;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.ClientInfoTask;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.response.ProfileUpdateReviewResponse.ChangeEntry;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.response.ProfileUpdateReviewResponse.FieldDiff;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.response.ProfileUpdateReviewResponse.FieldValue;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client_info_task.dto.response.ProfileUpdateReviewResponse.SectionDiff;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ProfileDiffService {

    // ── Public API ──────────────────────────────────────────────────────────

    public List<SectionDiff> computeDiff(ClientInfo current, ClientInfoTask submitted) {
        List<SectionDiff> sections = new ArrayList<>();
        addIfPresent(sections, diffMainDetails(current.getMainDetails(), submitted.getMainDetails()));
        addIfPresent(sections, diffClientInformation(current.getClientInformation(), submitted.getClientInformation()));
        addIfPresent(sections, diffCorporateOfficerInfo(current.getCorporateOfficerInformation(), submitted.getCorporateOfficerInformation()));
        addIfPresent(sections, diffAccessCredentials(current.getAccessCredentials(), submitted.getAccessCredentials()));
        addIfPresent(sections, diffScopeOfEngagement(current.getScopeOfEngagement(), submitted.getScopeOfEngagement()));
        addIfPresent(sections, diffProfessionalFees(current.getProfessionalFees(), submitted.getProfessionalFees()));
        addIfPresent(sections, diffOnboardingDetails(current.getOnboardingDetails(), submitted.getOnboardingDetails()));
        return sections;
    }


    // ── Section Diffs ───────────────────────────────────────────────────────

    private SectionDiff diffMainDetails(MainDetails c, MainDetails s) {
        if (c == null && s == null) return null;
        var cc = c != null ? c : new MainDetails(null, null);
        var ss = s != null ? s : new MainDetails(null, null);

        List<ChangeEntry> changes = new ArrayList<>();
        field(changes, "MRE Code", fmt(cc.mreCode()), fmt(ss.mreCode()));
        field(changes, "Commencement of Work", fmt(cc.commencementOfWork()), fmt(ss.commencementOfWork()));
        return toSection(SectionMetadata.MAIN_DETAILS, changes);
    }

    private SectionDiff diffClientInformation(ClientInformation c, ClientInformation s) {
        if (c == null && s == null) return null;
        var cc = c != null ? c : emptyClientInformation();
        var ss = s != null ? s : emptyClientInformation();

        List<ChangeEntry> changes = new ArrayList<>();

        // Header fields
        field(changes, "Registered Name", fmt(cc.registeredName()), fmt(ss.registeredName()));
        field(changes, "Trade Name", fmt(cc.tradeName()), fmt(ss.tradeName()));
        field(changes, "Number of Branches", fmt(cc.numberOfBranches()), fmt(ss.numberOfBranches()));
        field(changes, "Organization Type", fmtEnum(cc.organizationType()), fmtEnum(ss.organizationType()));

        // BIR Main Branch
        diffBirBranch(changes, "BIR Main Branch", cc.birMainBranch(), ss.birMainBranch());

        // BIR Branches list
        diffList(changes, safe(cc.birBranches()), safe(ss.birBranches()),
                b -> b.tin() != null ? b.tin() : b.businessTradeName(),
                b -> b.businessTradeName() != null ? b.businessTradeName() : "BIR Branch",
                this::compareBirBranch, this::listBirBranch);

        // BIR Tax Compliance
        diffBirTaxCompliance(changes, cc.birTaxCompliance(), ss.birTaxCompliance());

        // BIR Compliance Breakdown
        diffBirComplianceBreakdown(changes, cc.birComplianceBreakdown(), ss.birComplianceBreakdown());

        // DTI Details
        diffDtiDetails(changes, cc.dtiDetails(), ss.dtiDetails());

        // SEC Details
        diffSecDetails(changes, cc.secDetails(), ss.secDetails());

        // Government Agency Details
        diffGovAgency(changes, "SSS", cc.sssDetails(), ss.sssDetails());
        diffGovAgency(changes, "PhilHealth", cc.philhealthDetails(), ss.philhealthDetails());
        diffGovAgency(changes, "HDMF", cc.hdmfDetails(), ss.hdmfDetails());

        // City Hall Details list
        diffList(changes, safe(cc.cityHallDetails()), safe(ss.cityHallDetails()),
                CityHallDetails::businessPermitCity,
                ch -> ch.businessPermitCity() != null ? ch.businessPermitCity() : "City Hall",
                this::compareCityHall, this::listCityHall);

        return toSection(SectionMetadata.CLIENT_INFORMATION, changes);
    }

    private SectionDiff diffCorporateOfficerInfo(CorporateOfficerInformation c, CorporateOfficerInformation s) {
        if (c == null && s == null) return null;
        var cc = c != null ? c : new CorporateOfficerInformation(null, null);
        var ss = s != null ? s : new CorporateOfficerInformation(null, null);

        List<ChangeEntry> changes = new ArrayList<>();

        // Officers list
        diffList(changes, safe(cc.officers()), safe(ss.officers()),
                CorporateOfficerDetails::name,
                o -> o.name() != null ? o.name() : "Officer",
                this::compareOfficer, this::listOfficer);

        // Point of Contact
        diffPointOfContact(changes, cc.pointOfContact(), ss.pointOfContact());

        return toSection(SectionMetadata.CORPORATE_OFFICER_INFORMATION, changes);
    }

    private SectionDiff diffAccessCredentials(List<AccessCredentialDetails> c, List<AccessCredentialDetails> s) {
        if (c == null && s == null) return null;
        List<ChangeEntry> changes = new ArrayList<>();
        diffList(changes, safe(c), safe(s),
                AccessCredentialDetails::platform,
                ac -> ac.platform() != null ? ac.platform() : "Credential",
                this::compareCredential, this::listCredential);
        return toSection(SectionMetadata.ACCESS_CREDENTIALS, changes);
    }

    private SectionDiff diffScopeOfEngagement(ScopeOfEngagementDetails c, ScopeOfEngagementDetails s) {
        if (c == null && s == null) return null;
        var cc = c != null ? c : emptyScopeOfEngagement();
        var ss = s != null ? s : emptyScopeOfEngagement();

        List<ChangeEntry> changes = new ArrayList<>();

        // Header
        field(changes, "Date of Engagement Letter", fmt(cc.dateOfEngagementLetter()), fmt(ss.dateOfEngagementLetter()));
        field(changes, "Engagement Letters", fmtFiles(cc.engagementLetters()), fmtFiles(ss.engagementLetters()));

        // A. Documents & Information Gathering
        field(changes, "Sales Invoices and Documents", fmtRichText(cc.salesInvoicesAndDocuments()), fmtRichText(ss.salesInvoicesAndDocuments()));
        field(changes, "Purchase and Expense Documents", fmtRichText(cc.purchaseAndExpenseDocuments()), fmtRichText(ss.purchaseAndExpenseDocuments()));
        field(changes, "Payroll Documents", fmtRichText(cc.payrollDocuments()), fmtRichText(ss.payrollDocuments()));
        field(changes, "SSS, PhilHealth, HDMF Documents", fmtRichText(cc.sssPhilhealthHdmfDocuments()), fmtRichText(ss.sssPhilhealthHdmfDocuments()));
        field(changes, "Business Permits, Licenses & Other Documents", fmtRichText(cc.businessPermitsLicensesAndOtherDocuments()), fmtRichText(ss.businessPermitsLicensesAndOtherDocuments()));
        field(changes, "Additional Notes", fmtRichText(cc.additionalNotes()), fmtRichText(ss.additionalNotes()));

        // B. Client Engagements
        field(changes, "Tax Compliance", fmtRichText(cc.taxCompliance()), fmtRichText(ss.taxCompliance()));
        field(changes, "Book of Accounts", fmtEnum(cc.bookOfAccounts()), fmtEnum(ss.bookOfAccounts()));
        field(changes, "Bookkeeping Permit No.", fmt(cc.bookkeepingPermitNo()), fmt(ss.bookkeepingPermitNo()));
        field(changes, "Looseleaf Certificate & BIR Templates", fmtFiles(cc.looseleafCertificateAndBirTemplates()), fmtFiles(ss.looseleafCertificateAndBirTemplates()));

        // Registered Books list
        diffList(changes, safe(cc.registeredBooks()), safe(ss.registeredBooks()),
                RegisteredBookEntry::bookName,
                rb -> rb.bookName() != null ? rb.bookName() : "Book",
                this::compareRegisteredBook, this::listRegisteredBook);

        field(changes, "Bookkeeping Process", fmtRichText(cc.bookkeepingProcess()), fmtRichText(ss.bookkeepingProcess()));
        field(changes, "SSS, PhilHealth, HDMF Engagement", fmtRichText(cc.sssPhilhealthHdmfEngagement()), fmtRichText(ss.sssPhilhealthHdmfEngagement()));
        field(changes, "Payment Assistance", fmtRichText(cc.paymentAssistance()), fmtRichText(ss.paymentAssistance()));
        field(changes, "Consultation Free Allowance", fmt(cc.consultationFreeAllowance()), fmt(ss.consultationFreeAllowance()));
        field(changes, "Consultation Excess Rate", fmt(cc.consultationExcessRate()), fmt(ss.consultationExcessRate()));

        // C. Required Deliverable & Report
        field(changes, "Standard Deliverable", fmtRichText(cc.standardDeliverable()), fmtRichText(ss.standardDeliverable()));
        field(changes, "Required Deliverable Others", fmt(cc.requiredDeliverableOthers()), fmt(ss.requiredDeliverableOthers()));

        return toSection(SectionMetadata.SCOPE_OF_ENGAGEMENT, changes);
    }

    private SectionDiff diffProfessionalFees(List<ProfessionalFeeEntry> c, List<ProfessionalFeeEntry> s) {
        if (c == null && s == null) return null;
        List<ChangeEntry> changes = new ArrayList<>();
        diffList(changes, safe(c), safe(s),
                ProfessionalFeeEntry::serviceName,
                pf -> pf.serviceName() != null ? pf.serviceName() : "Fee",
                this::compareProfessionalFee, this::listProfessionalFee);
        return toSection(SectionMetadata.PROFESSIONAL_FEES, changes);
    }

    private SectionDiff diffOnboardingDetails(OnboardingDetails c, OnboardingDetails s) {
        if (c == null && s == null) return null;
        var cc = c != null ? c : new OnboardingDetails(null, null, null, null, null);
        var ss = s != null ? s : new OnboardingDetails(null, null, null, null, null);

        List<ChangeEntry> changes = new ArrayList<>();
        field(changes, "Name of Group Chat", fmt(cc.nameOfGroupChat()), fmt(ss.nameOfGroupChat()));
        field(changes, "Platform Used", fmt(cc.platformUsed()), fmt(ss.platformUsed()));
        field(changes, "GC Created By", fmt(cc.gcCreatedBy()), fmt(ss.gcCreatedBy()));
        field(changes, "GC Created Date", fmt(cc.gcCreatedDate()), fmt(ss.gcCreatedDate()));

        // Pending Action Items list
        diffList(changes, safe(cc.pendingActionItems()), safe(ss.pendingActionItems()),
                PendingActionItem::particulars,
                pa -> pa.particulars() != null ? pa.particulars() : "Action Item",
                this::compareActionItem, this::listActionItem);

        return toSection(SectionMetadata.ONBOARDING_DETAILS, changes);
    }

    // ── Sub-object Diffs ────────────────────────────────────────────────────

    private void diffBirBranch(List<ChangeEntry> changes, String prefix, BirBranchDetails c, BirBranchDetails s) {
        var cc = c != null ? c : emptyBirBranch();
        var ss = s != null ? s : emptyBirBranch();
        field(changes, prefix + " - Business/Trade Name", fmt(cc.businessTradeName()), fmt(ss.businessTradeName()));
        field(changes, prefix + " - TIN", fmt(cc.tin()), fmt(ss.tin()));
        field(changes, prefix + " - RDO", fmt(cc.rdo()), fmt(ss.rdo()));
        field(changes, prefix + " - Complete Registered Address", fmt(cc.completeRegisteredAddress()), fmt(ss.completeRegisteredAddress()));
        field(changes, prefix + " - BIR Registration Number", fmt(cc.birRegistrationNumber()), fmt(ss.birRegistrationNumber()));
        field(changes, prefix + " - Type of Business", fmt(cc.typeOfBusiness()), fmt(ss.typeOfBusiness()));
        field(changes, prefix + " - Classification", fmt(cc.classification()), fmt(ss.classification()));
        field(changes, prefix + " - Date of BIR Registration", fmt(cc.dateOfBirRegistration()), fmt(ss.dateOfBirRegistration()));
        field(changes, prefix + " - BIR Certificate of Registration", fmt(cc.birCertificateOfRegistration()), fmt(ss.birCertificateOfRegistration()));
        field(changes, prefix + " - BIR Form 1901", fmt(cc.birForm1901()), fmt(ss.birForm1901()));
        field(changes, prefix + " - BIR Form 1921 ATP", fmt(cc.birForm1921Atp()), fmt(ss.birForm1921Atp()));
        field(changes, prefix + " - BIR Form 1905", fmt(cc.birForm1905()), fmt(ss.birForm1905()));
        field(changes, prefix + " - Sample Invoice/Receipts", fmt(cc.sampleInvoiceReceipts()), fmt(ss.sampleInvoiceReceipts()));
        field(changes, prefix + " - NIRI Poster", fmt(cc.niriPoster()), fmt(ss.niriPoster()));
        field(changes, prefix + " - BIR Book of Accounts Stamp", fmt(cc.birBookOfAccountsStamp()), fmt(ss.birBookOfAccountsStamp()));
        field(changes, prefix + " - BIR Form 2000 DST", fmt(cc.birForm2000Dst()), fmt(ss.birForm2000Dst()));
        field(changes, prefix + " - Contract of Lease", fmt(cc.contractOfLease()), fmt(ss.contractOfLease()));
    }

    private void diffBirTaxCompliance(List<ChangeEntry> changes, BirTaxComplianceDetails c, BirTaxComplianceDetails s) {
        var cc = c != null ? c : new BirTaxComplianceDetails(null, null, false, null, null);
        var ss = s != null ? s : new BirTaxComplianceDetails(null, null, false, null, null);

        // Gross Sales list
        diffList(changes, safe(cc.grossSales()), safe(ss.grossSales()),
                gs -> String.valueOf(gs.year()),
                gs -> "Year " + gs.year(),
                this::compareGrossSales, this::listGrossSales);

        field(changes, "Taxpayer Classification", fmtTaxpayerClassification(cc.taxpayerClassification()), fmtTaxpayerClassification(ss.taxpayerClassification()));
        field(changes, "TOP Withholding", fmtBool(cc.topWithholding()), fmtBool(ss.topWithholding()));
        field(changes, "Date Classified TOP Withholding", fmt(cc.dateClassifiedTopWithholding()), fmt(ss.dateClassifiedTopWithholding()));
        field(changes, "Income Tax Regime", fmt(cc.incomeTaxRegime()), fmt(ss.incomeTaxRegime()));
    }

    private void diffBirComplianceBreakdown(List<ChangeEntry> changes, BirComplianceBreakdown c, BirComplianceBreakdown s) {
        var cc = c != null ? c : new BirComplianceBreakdown(null, null);
        var ss = s != null ? s : new BirComplianceBreakdown(null, null);

        diffList(changes, safe(cc.items()), safe(ss.items()),
                BirComplianceItem::taxReturnName,
                bi -> bi.taxReturnName() != null ? bi.taxReturnName() : "Compliance Item",
                this::compareComplianceItem, this::listComplianceItem);

        field(changes, "Compliance Breakdown - Others", fmt(cc.othersSpecify()), fmt(ss.othersSpecify()));
    }

    private void diffDtiDetails(List<ChangeEntry> changes, DtiDetails c, DtiDetails s) {
        var cc = c != null ? c : emptyDtiDetails();
        var ss = s != null ? s : emptyDtiDetails();
        field(changes, "DTI Registration No.", fmt(cc.dtiRegistrationNo()), fmt(ss.dtiRegistrationNo()));
        field(changes, "DTI Date of Registration", fmt(cc.dtiDateOfRegistration()), fmt(ss.dtiDateOfRegistration()));
        field(changes, "DTI Date of Expiration", fmt(cc.dtiDateOfExpiration()), fmt(ss.dtiDateOfExpiration()));
        field(changes, "DTI Business Registration Certificate", fmt(cc.dtiBusinessRegistrationCertificate()), fmt(ss.dtiBusinessRegistrationCertificate()));
        field(changes, "DTI BNRS Undertaking Form", fmt(cc.dtiBnrsUndertakingForm()), fmt(ss.dtiBnrsUndertakingForm()));
        field(changes, "DTI Official Receipt", fmt(cc.dtiOfficialReceipt()), fmt(ss.dtiOfficialReceipt()));
        field(changes, "BMBE Total Assets", fmt(cc.bmbeTotalAssets()), fmt(ss.bmbeTotalAssets()));
        field(changes, "BMBE No.", fmt(cc.bmbeNo()), fmt(ss.bmbeNo()));
        field(changes, "BMBE Date of Registration", fmt(cc.bmbeDateOfRegistration()), fmt(ss.bmbeDateOfRegistration()));
        field(changes, "BMBE Date of Expiration", fmt(cc.bmbeDateOfExpiration()), fmt(ss.bmbeDateOfExpiration()));
        field(changes, "BMBE Official Receipt", fmt(cc.bmbeOfficialReceipt()), fmt(ss.bmbeOfficialReceipt()));
        field(changes, "DTI Others", fmt(cc.others()), fmt(ss.others()));
    }

    private void diffSecDetails(List<ChangeEntry> changes, SecDetails c, SecDetails s) {
        var cc = c != null ? c : emptySecDetails();
        var ss = s != null ? s : emptySecDetails();
        field(changes, "SEC - Date of Incorporation", fmt(cc.dateOfIncorporation()), fmt(ss.dateOfIncorporation()));
        field(changes, "SEC Registration Number", fmt(cc.secRegistrationNumber()), fmt(ss.secRegistrationNumber()));
        field(changes, "SEC - Date of Actual Meeting per Bylaws", fmt(cc.dateOfActualMeetingPerBylaws()), fmt(ss.dateOfActualMeetingPerBylaws()));
        field(changes, "SEC - Primary Purpose per Articles", fmt(cc.primaryPurposePerArticles()), fmt(ss.primaryPurposePerArticles()));
        field(changes, "SEC - Corporation Category", fmt(cc.corporationCategory()), fmt(ss.corporationCategory()));
        field(changes, "SEC Certificate of Incorporation", fmt(cc.secCertificateOfIncorporation()), fmt(ss.secCertificateOfIncorporation()));
        field(changes, "Articles of Incorporation", fmt(cc.articlesOfIncorporation()), fmt(ss.articlesOfIncorporation()));
        field(changes, "Bylaws of Corporation", fmt(cc.bylawsOfCorporation()), fmt(ss.bylawsOfCorporation()));
        field(changes, "Certificate of Authentication", fmt(cc.certificateOfAuthentication()), fmt(ss.certificateOfAuthentication()));
        field(changes, "Authorize Filer / Secretary Certificate", fmt(cc.authorizeFilerSecretaryCertificate()), fmt(ss.authorizeFilerSecretaryCertificate()));
        field(changes, "SEC Official Receipts", fmt(cc.secOfficialReceipts()), fmt(ss.secOfficialReceipts()));
        field(changes, "Latest GIS / Appointment of Officer", fmt(cc.latestGisOrAppointmentOfOfficer()), fmt(ss.latestGisOrAppointmentOfOfficer()));
        field(changes, "Stock and Transfer Book", fmt(cc.stockAndTransferBook()), fmt(ss.stockAndTransferBook()));
        field(changes, "Board Resolutions / Secretary Certificate", fmt(cc.boardResolutionsSecretaryCertificate()), fmt(ss.boardResolutionsSecretaryCertificate()));
        field(changes, "Previous Year AFS and ITR", fmt(cc.previousYearAfsAndItr()), fmt(ss.previousYearAfsAndItr()));
        field(changes, "SEC Others", fmt(cc.others()), fmt(ss.others()));
    }

    private void diffGovAgency(List<ChangeEntry> changes, String prefix, GovernmentAgencyDetails c, GovernmentAgencyDetails s) {
        var cc = c != null ? c : new GovernmentAgencyDetails(null, null, null, null);
        var ss = s != null ? s : new GovernmentAgencyDetails(null, null, null, null);
        field(changes, prefix + " - Date of Registration", fmt(cc.dateOfRegistration()), fmt(ss.dateOfRegistration()));
        field(changes, prefix + " - Registration Number", fmt(cc.registrationNumber()), fmt(ss.registrationNumber()));
        field(changes, prefix + " - Certificates and Documents", fmt(cc.certificatesAndDocuments()), fmt(ss.certificatesAndDocuments()));
        field(changes, prefix + " - Others", fmt(cc.others()), fmt(ss.others()));
    }

    private void diffPointOfContact(List<ChangeEntry> changes, PointOfContactDetails c, PointOfContactDetails s) {
        var cc = c != null ? c : emptyPointOfContact();
        var ss = s != null ? s : emptyPointOfContact();
        field(changes, "Contact Person", fmt(cc.contactPerson()), fmt(ss.contactPerson()));
        field(changes, "Contact Number", fmt(cc.contactNumber()), fmt(ss.contactNumber()));
        field(changes, "Delivery Address", fmt(cc.deliveryAddress()), fmt(ss.deliveryAddress()));
        field(changes, "Landmark / Pin Location", fmt(cc.landmarkPinLocation()), fmt(ss.landmarkPinLocation()));
        field(changes, "Email Address", fmt(cc.emailAddress()), fmt(ss.emailAddress()));
        field(changes, "Preferred Method of Communication", fmt(cc.preferredMethodOfCommunication()), fmt(ss.preferredMethodOfCommunication()));
        field(changes, "Alternative Contact", fmt(cc.alternativeContact()), fmt(ss.alternativeContact()));
    }

    // ── Generic List Differ ─────────────────────────────────────────────────

    private <T> void diffList(
            List<ChangeEntry> changes,
            List<T> current,
            List<T> submitted,
            Function<T, String> keyExtractor,
            Function<T, String> labelExtractor,
            BiFunction<T, T, List<FieldDiff>> fieldComparator,
            Function<T, List<FieldValue>> fieldLister
    ) {
        Map<String, T> currentByKey = new LinkedHashMap<>();
        Map<String, T> submittedByKey = new LinkedHashMap<>();

        indexByKey(current, keyExtractor, labelExtractor, currentByKey);
        indexByKey(submitted, keyExtractor, labelExtractor, submittedByKey);

        // Modified + Removed
        for (var entry : currentByKey.entrySet()) {
            String key = entry.getKey();
            T cItem = entry.getValue();
            T sItem = submittedByKey.get(key);
            if (sItem != null) {
                List<FieldDiff> diffs = fieldComparator.apply(cItem, sItem);
                if (!diffs.isEmpty()) {
                    changes.add(new ChangeEntry("modified", null, null, null, null,
                            labelExtractor.apply(sItem), diffs));
                }
            } else {
                changes.add(new ChangeEntry("removed", null, null, null, null,
                        labelExtractor.apply(cItem), null));
            }
        }

        // Added
        for (var entry : submittedByKey.entrySet()) {
            if (!currentByKey.containsKey(entry.getKey())) {
                T sItem = entry.getValue();
                List<FieldValue> vals = fieldLister.apply(sItem);
                List<FieldDiff> asFields = vals.stream()
                        .map(fv -> new FieldDiff(fv.field(), null, fv.value()))
                        .toList();
                changes.add(new ChangeEntry("added", null, null, null, null,
                        labelExtractor.apply(sItem), asFields));
            }
        }
    }

    private <T> void indexByKey(List<T> items, Function<T, String> keyExtractor,
                                Function<T, String> labelExtractor, Map<String, T> target) {
        int index = 0;
        for (T item : items) {
            String key = keyExtractor.apply(item);
            if (key == null || key.isBlank()) {
                key = "__pos_" + index;
            }
            if (target.containsKey(key)) {
                key = key + "__dup_" + index;
            }
            target.put(key, item);
            index++;
        }
    }

    // ── Item Comparators ────────────────────────────────────────────────────

    private List<FieldDiff> compareOfficer(CorporateOfficerDetails c, CorporateOfficerDetails s) {
        List<FieldDiff> d = new ArrayList<>();
        diff(d, "Name", fmt(c.name()), fmt(s.name()));
        diff(d, "Birthday", fmt(c.birthday()), fmt(s.birthday()));
        diff(d, "Address", fmt(c.address()), fmt(s.address()));
        diff(d, "Position", fmt(c.position()), fmt(s.position()));
        diff(d, "ID Scanned with 3 Signatures", fmt(c.idScannedWith3Signature()), fmt(s.idScannedWith3Signature()));
        return d;
    }

    private List<FieldValue> listOfficer(CorporateOfficerDetails o) {
        return listNonNull(
                fv("Name", fmt(o.name())),
                fv("Birthday", fmt(o.birthday())),
                fv("Address", fmt(o.address())),
                fv("Position", fmt(o.position())),
                fv("ID Scanned with 3 Signatures", fmt(o.idScannedWith3Signature()))
        );
    }

    private List<FieldDiff> compareCredential(AccessCredentialDetails c, AccessCredentialDetails s) {
        List<FieldDiff> d = new ArrayList<>();
        diff(d, "Platform", fmt(c.platform()), fmt(s.platform()));
        diff(d, "Link to Platform", fmt(c.linkToPlatform()), fmt(s.linkToPlatform()));
        diff(d, "Username / Email", fmt(c.usernameOrEmail()), fmt(s.usernameOrEmail()));
        diff(d, "Password", fmt(c.password()), fmt(s.password()));
        diff(d, "Notes", fmt(c.notes()), fmt(s.notes()));
        return d;
    }

    private List<FieldValue> listCredential(AccessCredentialDetails a) {
        return listNonNull(
                fv("Platform", fmt(a.platform())),
                fv("Link to Platform", fmt(a.linkToPlatform())),
                fv("Username / Email", fmt(a.usernameOrEmail())),
                fv("Password", fmt(a.password())),
                fv("Notes", fmt(a.notes()))
        );
    }

    private List<FieldDiff> compareBirBranch(BirBranchDetails c, BirBranchDetails s) {
        List<FieldDiff> d = new ArrayList<>();
        diff(d, "Business/Trade Name", fmt(c.businessTradeName()), fmt(s.businessTradeName()));
        diff(d, "TIN", fmt(c.tin()), fmt(s.tin()));
        diff(d, "RDO", fmt(c.rdo()), fmt(s.rdo()));
        diff(d, "Complete Registered Address", fmt(c.completeRegisteredAddress()), fmt(s.completeRegisteredAddress()));
        diff(d, "BIR Registration Number", fmt(c.birRegistrationNumber()), fmt(s.birRegistrationNumber()));
        diff(d, "Type of Business", fmt(c.typeOfBusiness()), fmt(s.typeOfBusiness()));
        diff(d, "Classification", fmt(c.classification()), fmt(s.classification()));
        diff(d, "Date of BIR Registration", fmt(c.dateOfBirRegistration()), fmt(s.dateOfBirRegistration()));
        diff(d, "BIR Certificate of Registration", fmt(c.birCertificateOfRegistration()), fmt(s.birCertificateOfRegistration()));
        diff(d, "BIR Form 1901", fmt(c.birForm1901()), fmt(s.birForm1901()));
        diff(d, "BIR Form 1921 ATP", fmt(c.birForm1921Atp()), fmt(s.birForm1921Atp()));
        diff(d, "BIR Form 1905", fmt(c.birForm1905()), fmt(s.birForm1905()));
        diff(d, "Sample Invoice/Receipts", fmt(c.sampleInvoiceReceipts()), fmt(s.sampleInvoiceReceipts()));
        diff(d, "NIRI Poster", fmt(c.niriPoster()), fmt(s.niriPoster()));
        diff(d, "BIR Book of Accounts Stamp", fmt(c.birBookOfAccountsStamp()), fmt(s.birBookOfAccountsStamp()));
        diff(d, "BIR Form 2000 DST", fmt(c.birForm2000Dst()), fmt(s.birForm2000Dst()));
        diff(d, "Contract of Lease", fmt(c.contractOfLease()), fmt(s.contractOfLease()));
        return d;
    }

    private List<FieldValue> listBirBranch(BirBranchDetails b) {
        return listNonNull(
                fv("Business/Trade Name", fmt(b.businessTradeName())),
                fv("TIN", fmt(b.tin())),
                fv("RDO", fmt(b.rdo())),
                fv("Complete Registered Address", fmt(b.completeRegisteredAddress())),
                fv("BIR Registration Number", fmt(b.birRegistrationNumber())),
                fv("Type of Business", fmt(b.typeOfBusiness())),
                fv("Classification", fmt(b.classification())),
                fv("Date of BIR Registration", fmt(b.dateOfBirRegistration())),
                fv("BIR Certificate of Registration", fmt(b.birCertificateOfRegistration())),
                fv("BIR Form 1901", fmt(b.birForm1901())),
                fv("BIR Form 1921 ATP", fmt(b.birForm1921Atp())),
                fv("BIR Form 1905", fmt(b.birForm1905())),
                fv("Sample Invoice/Receipts", fmt(b.sampleInvoiceReceipts())),
                fv("NIRI Poster", fmt(b.niriPoster())),
                fv("BIR Book of Accounts Stamp", fmt(b.birBookOfAccountsStamp())),
                fv("BIR Form 2000 DST", fmt(b.birForm2000Dst())),
                fv("Contract of Lease", fmt(b.contractOfLease()))
        );
    }

    private List<FieldDiff> compareCityHall(CityHallDetails c, CityHallDetails s) {
        List<FieldDiff> d = new ArrayList<>();
        diff(d, "Business Permit City", fmt(c.businessPermitCity()), fmt(s.businessPermitCity()));
        diff(d, "Business Permit Number", fmt(c.businessPermitNumber()), fmt(s.businessPermitNumber()));
        diff(d, "Date of Registration", fmt(c.dateOfRegistration()), fmt(s.dateOfRegistration()));
        diff(d, "Renewal Basis", fmt(c.renewalBasis()), fmt(s.renewalBasis()));
        diff(d, "Quarterly Deadline Q2", fmt(c.quarterlyDeadlineQ2()), fmt(s.quarterlyDeadlineQ2()));
        diff(d, "Quarterly Deadline Q3", fmt(c.quarterlyDeadlineQ3()), fmt(s.quarterlyDeadlineQ3()));
        diff(d, "Quarterly Deadline Q4", fmt(c.quarterlyDeadlineQ4()), fmt(s.quarterlyDeadlineQ4()));
        diff(d, "Permit Expiration Date", fmt(c.permitExpirationDate()), fmt(s.permitExpirationDate()));
        diff(d, "Fire Permit", fmtPermit(c.firePermit()), fmtPermit(s.firePermit()));
        diff(d, "Sanitary Permit", fmtPermit(c.sanitaryPermit()), fmtPermit(s.sanitaryPermit()));
        diff(d, "Other Permit", fmtPermit(c.otherPermit()), fmtPermit(s.otherPermit()));
        diff(d, "Mayor's Business Permit", fmt(c.mayorBusinessPermit()), fmt(s.mayorBusinessPermit()));
        diff(d, "Business Permit Plate", fmt(c.businessPermitPlate()), fmt(s.businessPermitPlate()));
        diff(d, "Billing Assessment", fmt(c.billingAssessment()), fmt(s.billingAssessment()));
        diff(d, "Official Receipt of Payment", fmt(c.officialReceiptOfPayment()), fmt(s.officialReceiptOfPayment()));
        diff(d, "Sanitary Permit File", fmt(c.sanitaryPermitFile()), fmt(s.sanitaryPermitFile()));
        diff(d, "Fire Permit File", fmt(c.firePermitFile()), fmt(s.firePermitFile()));
        diff(d, "Barangay Permit", fmt(c.barangayPermit()), fmt(s.barangayPermit()));
        diff(d, "Community Tax Certificate", fmt(c.communityTaxCertificate()), fmt(s.communityTaxCertificate()));
        diff(d, "Locational Clearance", fmt(c.locationalClearance()), fmt(s.locationalClearance()));
        diff(d, "Environmental Clearance", fmt(c.environmentalClearance()), fmt(s.environmentalClearance()));
        diff(d, "Comprehensive General Liability Insurance", fmt(c.comprehensiveGeneralLiabilityInsurance()), fmt(s.comprehensiveGeneralLiabilityInsurance()));
        return d;
    }

    private List<FieldValue> listCityHall(CityHallDetails ch) {
        return listNonNull(
                fv("Business Permit City", fmt(ch.businessPermitCity())),
                fv("Business Permit Number", fmt(ch.businessPermitNumber())),
                fv("Date of Registration", fmt(ch.dateOfRegistration())),
                fv("Renewal Basis", fmt(ch.renewalBasis())),
                fv("Permit Expiration Date", fmt(ch.permitExpirationDate())),
                fv("Fire Permit", fmtPermit(ch.firePermit())),
                fv("Sanitary Permit", fmtPermit(ch.sanitaryPermit()))
        );
    }

    private List<FieldDiff> compareGrossSales(GrossSalesEntry c, GrossSalesEntry s) {
        List<FieldDiff> d = new ArrayList<>();
        diff(d, "Year", fmt(c.year()), fmt(s.year()));
        diff(d, "Amount", fmt(c.amount()), fmt(s.amount()));
        return d;
    }

    private List<FieldValue> listGrossSales(GrossSalesEntry gs) {
        return listNonNull(fv("Year", fmt(gs.year())), fv("Amount", fmt(gs.amount())));
    }

    private List<FieldDiff> compareComplianceItem(BirComplianceItem c, BirComplianceItem s) {
        List<FieldDiff> d = new ArrayList<>();
        diff(d, "Category", fmt(c.category()), fmt(s.category()));
        diff(d, "Tax Return Name", fmt(c.taxReturnName()), fmt(s.taxReturnName()));
        diff(d, "Deadline", fmt(c.deadline()), fmt(s.deadline()));
        diff(d, "Applicable", fmtBool(c.applicable()), fmtBool(s.applicable()));
        diff(d, "Notes", fmt(c.notes()), fmt(s.notes()));
        return d;
    }

    private List<FieldValue> listComplianceItem(BirComplianceItem bi) {
        return listNonNull(
                fv("Category", fmt(bi.category())),
                fv("Tax Return Name", fmt(bi.taxReturnName())),
                fv("Deadline", fmt(bi.deadline())),
                fv("Applicable", fmtBool(bi.applicable())),
                fv("Notes", fmt(bi.notes()))
        );
    }

    private List<FieldDiff> compareProfessionalFee(ProfessionalFeeEntry c, ProfessionalFeeEntry s) {
        List<FieldDiff> d = new ArrayList<>();
        diff(d, "Service Name", fmt(c.serviceName()), fmt(s.serviceName()));
        diff(d, "Fee", fmt(c.fee()), fmt(s.fee()));
        return d;
    }

    private List<FieldValue> listProfessionalFee(ProfessionalFeeEntry pf) {
        return listNonNull(fv("Service Name", fmt(pf.serviceName())), fv("Fee", fmt(pf.fee())));
    }

    private List<FieldDiff> compareRegisteredBook(RegisteredBookEntry c, RegisteredBookEntry s) {
        List<FieldDiff> d = new ArrayList<>();
        diff(d, "Book Name", fmt(c.bookName()), fmt(s.bookName()));
        diff(d, "Notes", fmt(c.notes()), fmt(s.notes()));
        return d;
    }

    private List<FieldValue> listRegisteredBook(RegisteredBookEntry rb) {
        return listNonNull(fv("Book Name", fmt(rb.bookName())), fv("Notes", fmt(rb.notes())));
    }

    private List<FieldDiff> compareActionItem(PendingActionItem c, PendingActionItem s) {
        List<FieldDiff> d = new ArrayList<>();
        diff(d, "Particulars", fmt(c.particulars()), fmt(s.particulars()));
        diff(d, "Notes", fmt(c.notes()), fmt(s.notes()));
        return d;
    }

    private List<FieldValue> listActionItem(PendingActionItem pa) {
        return listNonNull(fv("Particulars", fmt(pa.particulars())), fv("Notes", fmt(pa.notes())));
    }

    // ── Formatters ──────────────────────────────────────────────────────────

    private String fmt(String val) {
        return val;
    }

    private String fmt(int val) {
        return String.valueOf(val);
    }

    private String fmt(BigDecimal val) {
        return val != null ? val.toPlainString() : null;
    }

    private String fmt(DateField val) {
        if (val == null || val.date() == null) return null;
        return val.date().toString();
    }

    private String fmt(FileReference val) {
        if (val == null) return null;
        return val.name();
    }

    private String fmtFiles(List<FileReference> val) {
        if (val == null || val.isEmpty()) return null;
        return String.join(", ", val.stream().map(FileReference::name).toList());
    }

    private String fmt(LinkReference val) {
        if (val == null) return null;
        return val.label() != null ? val.label() : val.url();
    }

    private String fmtBool(boolean val) {
        return val ? "Yes" : "No";
    }

    private String fmtEnum(Enum<?> val) {
        if (val == null) return null;
        return val.name().replace('_', ' ');
    }

    private String fmtTaxpayerClassification(TaxpayerClassification val) {
        return val != null ? val.getDisplayName() : null;
    }

    private String fmtRichText(Map<String, Object> val) {
        if (val == null || val.isEmpty()) return null;
        return "[Rich text content]";
    }

    private String fmtPermit(PermitDetails val) {
        if (val == null) return null;
        String no = val.number() != null ? val.number() : "";
        String exp = fmt(val.expirationDate());
        if (no.isEmpty() && exp == null) return null;
        if (exp == null) return "No. " + no;
        if (no.isEmpty()) return "Expires: " + exp;
        return "No. " + no + ", Expires: " + exp;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void field(List<ChangeEntry> changes, String label, String oldVal, String newVal) {
        if (Objects.equals(oldVal, newVal)) return;
        changes.add(new ChangeEntry("field", label, oldVal, newVal, null, null, null));
    }

    private void diff(List<FieldDiff> diffs, String label, String oldVal, String newVal) {
        if (Objects.equals(oldVal, newVal)) return;
        diffs.add(new FieldDiff(label, oldVal, newVal));
    }

    private FieldValue fv(String field, String value) {
        return new FieldValue(field, value);
    }

    private List<FieldValue> listNonNull(FieldValue... values) {
        List<FieldValue> result = new ArrayList<>();
        for (FieldValue v : values) {
            if (v.value() != null) result.add(v);
        }
        return result;
    }

    private SectionDiff toSection(SectionMetadata meta, List<ChangeEntry> changes) {
        if (changes.isEmpty()) return null;
        return new SectionDiff(meta.getKey(), meta.getLabel(), changes);
    }

    private void addIfPresent(List<SectionDiff> sections, SectionDiff diff) {
        if (diff != null) sections.add(diff);
    }

    private <T> List<T> safe(List<T> list) {
        return list != null ? list : List.of();
    }

    // ── Empty Object Factories ──────────────────────────────────────────────

    private ClientInformation emptyClientInformation() {
        return new ClientInformation(null, null, 0, null, null, null, null, null, null, null, null, null, null, null);
    }

    private BirBranchDetails emptyBirBranch() {
        return new BirBranchDetails(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private DtiDetails emptyDtiDetails() {
        return new DtiDetails(null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private SecDetails emptySecDetails() {
        return new SecDetails(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private PointOfContactDetails emptyPointOfContact() {
        return new PointOfContactDetails(null, null, null, null, null, null, null);
    }

    private ScopeOfEngagementDetails emptyScopeOfEngagement() {
        return new ScopeOfEngagementDetails(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
