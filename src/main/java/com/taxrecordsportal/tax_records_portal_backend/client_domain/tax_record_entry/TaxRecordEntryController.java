package com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientRepository;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry.dto.response.DrillDownResponse;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry.dto.response.ImportantDateResponse;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry.dto.response.RecentTaxRecordEntryResponse;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry.dto.response.TaxRecordEntryResponse;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.Period;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static com.taxrecordsportal.tax_records_portal_backend.common.util.SecurityUtil.getCurrentUser;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TaxRecordEntryController {

    private final TaxRecordEntryService taxRecordEntryService;
    private final ImportantDateService importantDateService;
    private final ClientRepository clientRepository;

    @GetMapping("/clients/{clientId}/tax-records")
    @PreAuthorize("hasAuthority('tax_records.view.all') or hasAuthority('tax_records.view.own')")
    public ResponseEntity<List<TaxRecordEntryResponse>> getByClientId(@PathVariable UUID clientId) {
        return ResponseEntity.ok(taxRecordEntryService.getByClientId(clientId));
    }

    @GetMapping("/tax-records/me/drill-down")
    public ResponseEntity<DrillDownResponse> drillDown(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer subCategoryId,
            @RequestParam(required = false) Integer taskNameId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Period period) {

        User currentUser = getCurrentUser();
        UUID clientId = clientRepository.findClientIdByUserId(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        return ResponseEntity.ok(taxRecordEntryService.drillDown(clientId, categoryId, subCategoryId, taskNameId, year, period));
    }

    @GetMapping("/tax-records/me/recent")
    public ResponseEntity<List<RecentTaxRecordEntryResponse>> getRecent(
            @RequestParam(defaultValue = "7d") String range) {

        User currentUser = getCurrentUser();
        UUID clientId = clientRepository.findClientIdByUserId(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        return ResponseEntity.ok(taxRecordEntryService.getRecentForClient(clientId, range));
    }

    @GetMapping("/tax-records/me/important-dates")
    public ResponseEntity<List<ImportantDateResponse>> getImportantDates() {
        return ResponseEntity.ok(importantDateService.getImportantDates());
    }

}
