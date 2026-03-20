package com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientRepository;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry.dto.response.DrillDownItemResponse;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry.dto.response.DrillDownResponse;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry.dto.response.TaxRecordEntryResponse;
import com.taxrecordsportal.tax_records_portal_backend.file_domain.file.FileEntity;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.Period;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.TaxRecordTask;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.WorkingFileItem;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

import static com.taxrecordsportal.tax_records_portal_backend.common.util.SecurityUtil.getCurrentUser;

@Service
@RequiredArgsConstructor
public class TaxRecordEntryService {

    private final TaxRecordEntryRepository taxRecordEntryRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public List<TaxRecordEntryResponse> getByClientId(UUID clientId) {
        User currentUser = getCurrentUser();
        boolean hasViewAll = currentUser.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "tax_records.view.all"));

        if (!hasViewAll) {
            Client client = clientRepository.findWithAccountantsAndUserById(clientId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
            boolean isAssigned = client.getAccountants() != null
                    && client.getAccountants().stream().anyMatch(a -> a.getId().equals(currentUser.getId()));
            boolean isClient = client.getUser() != null
                    && client.getUser().getId().equals(currentUser.getId());
            if (!isAssigned && !isClient) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this client's tax records");
            }
        }

        return taxRecordEntryRepository.findByClientId(clientId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DrillDownResponse drillDown(UUID clientId, Integer categoryId, Integer subCategoryId,
                                       Integer taskNameId, Integer year, Period period) {
        // Level 6: all params provided → return the record
        if (categoryId != null && subCategoryId != null && taskNameId != null && year != null && period != null) {
            TaxRecordEntry entry = taxRecordEntryRepository.findRecordWithFiles(
                    clientId, categoryId, subCategoryId, taskNameId, year, period
            ).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tax record entry not found"));
            return DrillDownResponse.ofRecord(toResponse(entry));
        }

        // Level 5: year provided → return periods
        if (taskNameId != null && year != null) {
            List<DrillDownItemResponse> items = taxRecordEntryRepository
                    .drillDownPeriods(clientId, taskNameId, year).stream()
                    .map(p -> new DrillDownItemResponse(p.getId(), p.getLabel(), p.getCount()))
                    .toList();
            return DrillDownResponse.ofItems("period", items);
        }

        // Level 4: taskNameId provided → return years
        if (taskNameId != null) {
            List<DrillDownItemResponse> items = taxRecordEntryRepository
                    .drillDownYears(clientId, taskNameId).stream()
                    .map(p -> new DrillDownItemResponse(p.getId(), p.getLabel(), p.getCount()))
                    .toList();
            return DrillDownResponse.ofItems("year", items);
        }

        // Level 3: subCategoryId provided → return task names
        if (subCategoryId != null) {
            List<DrillDownItemResponse> items = taxRecordEntryRepository
                    .drillDownTaskNames(clientId, subCategoryId).stream()
                    .map(p -> new DrillDownItemResponse(p.getId(), p.getLabel(), p.getCount()))
                    .toList();
            return DrillDownResponse.ofItems("taskName", items);
        }

        // Level 2: categoryId provided → return sub-categories
        if (categoryId != null) {
            List<DrillDownItemResponse> items = taxRecordEntryRepository
                    .drillDownSubCategories(clientId, categoryId).stream()
                    .map(p -> new DrillDownItemResponse(p.getId(), p.getLabel(), p.getCount()))
                    .toList();
            return DrillDownResponse.ofItems("subCategory", items);
        }

        // Level 1: no params → return categories
        List<DrillDownItemResponse> items = taxRecordEntryRepository
                .drillDownCategories(clientId).stream()
                .map(p -> new DrillDownItemResponse(p.getId(), p.getLabel(), p.getCount()))
                .toList();
        return DrillDownResponse.ofItems("category", items);
    }

    @Transactional
    public void mergeFromTask(TaxRecordTask task) {
        Optional<TaxRecordEntry> existing = taxRecordEntryRepository
                .findByClientIdAndCategoryIdAndSubCategoryIdAndTaskNameIdAndYearAndPeriod(
                        task.getClient().getId(),
                        task.getCategory().getId(),
                        task.getSubCategory().getId(),
                        task.getTaskName().getId(),
                        task.getYear(),
                        task.getPeriod()
                );

        if (existing.isPresent()) {
            TaxRecordEntry entry = existing.get();

            // Append working files, avoiding duplicates by fileId
            List<WorkingFileItem> merged = mergeWorkingFiles(entry.getWorkingFiles(), task.getWorkingFiles());
            entry.setWorkingFiles(merged);

            // Update output and proof of filing files if task provides them
            if (task.getOutputFile() != null) {
                entry.setOutputFile(task.getOutputFile());
            }
            if (task.getProofOfFilingFile() != null) {
                entry.setProofOfFilingFile(task.getProofOfFilingFile());
            }

            taxRecordEntryRepository.save(entry);
        } else {
            TaxRecordEntry entry = new TaxRecordEntry();
            entry.setClient(task.getClient());
            entry.setCategory(task.getCategory());
            entry.setSubCategory(task.getSubCategory());
            entry.setTaskName(task.getTaskName());
            entry.setYear(task.getYear());
            entry.setPeriod(task.getPeriod());
            entry.setWorkingFiles(task.getWorkingFiles() != null ? new ArrayList<>(task.getWorkingFiles()) : new ArrayList<>());
            entry.setOutputFile(task.getOutputFile());
            entry.setProofOfFilingFile(task.getProofOfFilingFile());

            taxRecordEntryRepository.save(entry);
        }
    }

    private List<WorkingFileItem> mergeWorkingFiles(List<WorkingFileItem> existing, List<WorkingFileItem> incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return existing != null ? existing : new ArrayList<>();
        }
        if (existing == null || existing.isEmpty()) {
            return new ArrayList<>(incoming);
        }

        Set<UUID> existingFileIds = existing.stream()
                .map(WorkingFileItem::fileId)
                .collect(Collectors.toSet());

        List<WorkingFileItem> merged = new ArrayList<>(existing);
        for (WorkingFileItem item : incoming) {
            if (!existingFileIds.contains(item.fileId())) {
                merged.add(item);
            }
        }
        return merged;
    }

    private TaxRecordEntryResponse toResponse(TaxRecordEntry entry) {
        FileEntity output = entry.getOutputFile();
        FileEntity proof = entry.getProofOfFilingFile();

        List<TaxRecordEntryResponse.WorkingFileItem> workingFiles = entry.getWorkingFiles() != null
                ? entry.getWorkingFiles().stream()
                    .map(w -> new TaxRecordEntryResponse.WorkingFileItem(w.type(), w.fileId(), w.fileName(), w.url(), w.label()))
                    .toList()
                : List.of();

        return new TaxRecordEntryResponse(
                entry.getId(),
                entry.getCategory().getName(),
                entry.getSubCategory().getName(),
                entry.getTaskName().getName(),
                entry.getYear(),
                entry.getPeriod(),
                workingFiles,
                output != null ? new TaxRecordEntryResponse.FileRef(output.getId(), output.getName()) : null,
                proof != null ? new TaxRecordEntryResponse.FileRef(proof.getId(), proof.getName()) : null
        );
    }
}
