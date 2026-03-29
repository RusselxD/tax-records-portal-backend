package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_name;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry.TaxRecordEntryRepository;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.TaxRecordTaskRepository;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response.TaxRecordLookupResponse;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category.TaxTaskSubCategory;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category.TaxTaskSubCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxTaskNameService {

    private final TaxTaskNameRepository taxTaskNameRepository;
    private final TaxTaskSubCategoryService taxTaskSubCategoryService;
    private final TaxRecordTaskRepository taxRecordTaskRepository;
    private final TaxRecordEntryRepository taxRecordEntryRepository;

    @Transactional(readOnly = true)
    public List<TaxRecordLookupResponse> getAll() {
        return taxTaskNameRepository.findAll()
                .stream()
                .map(tn -> new TaxRecordLookupResponse(tn.getId(), tn.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaxRecordLookupResponse> getBySubCategory(Integer subCategoryId) {
        return taxTaskNameRepository.findBySubCategoryId(subCategoryId)
                .stream()
                .map(tn -> new TaxRecordLookupResponse(tn.getId(), tn.getName()))
                .toList();
    }

    @Transactional
    public TaxRecordLookupResponse create(Integer subCategoryId, String name) {
        TaxTaskSubCategory subCategory = taxTaskSubCategoryService.getById(subCategoryId);

        if (taxTaskNameRepository.existsByNameAndSubCategoryId(name, subCategoryId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task name already exists in this sub category");
        }

        TaxTaskName taskName = new TaxTaskName();
        taskName.setSubCategory(subCategory);
        taskName.setName(name);

        TaxTaskName saved = taxTaskNameRepository.save(taskName);
        return new TaxRecordLookupResponse(saved.getId(), saved.getName());
    }

    @Transactional
    public void delete(Integer subCategoryId, Integer taskNameId) {
        TaxTaskName taskName = taxTaskNameRepository.findById(taskNameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task name not found"));

        if (!taskName.getSubCategory().getId().equals(subCategoryId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task name not found in this sub category");
        }

        if (taxRecordTaskRepository.existsByTaskNameId(taskNameId)
                || taxRecordEntryRepository.existsByTaskNameId(taskNameId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task name is referenced and cannot be deleted");
        }

        taxTaskNameRepository.delete(taskName);
    }
}
