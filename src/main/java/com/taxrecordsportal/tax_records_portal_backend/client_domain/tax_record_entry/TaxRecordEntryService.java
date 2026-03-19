package com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.Client;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.client.ClientRepository;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry.dto.response.TaxRecordEntryResponse;
import com.taxrecordsportal.tax_records_portal_backend.file_domain.file.FileEntity;
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

        return new TaxRecordEntryResponse(
                entry.getId(),
                entry.getCategory().getName(),
                entry.getSubCategory().getName(),
                entry.getTaskName().getName(),
                entry.getYear(),
                entry.getPeriod(),
                entry.getWorkingFiles(),
                output != null ? output.getId() : null,
                output != null ? output.getName() : null,
                proof != null ? proof.getId() : null,
                proof != null ? proof.getName() : null
        );
    }
}
