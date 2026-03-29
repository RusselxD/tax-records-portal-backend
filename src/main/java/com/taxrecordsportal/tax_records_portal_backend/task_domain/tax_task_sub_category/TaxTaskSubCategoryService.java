package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry.TaxRecordEntryRepository;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.TaxRecordTaskRepository;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response.TaxRecordLookupResponse;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_category.TaxTaskCategory;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_category.TaxTaskCategoryService;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_name.TaxTaskNameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxTaskSubCategoryService {

    private final TaxTaskSubCategoryRepository taxTaskSubCategoryRepository;
    private final TaxTaskCategoryService taxTaskCategoryService;
    private final TaxTaskNameRepository taxTaskNameRepository;
    private final TaxRecordTaskRepository taxRecordTaskRepository;
    private final TaxRecordEntryRepository taxRecordEntryRepository;

    @Transactional(readOnly = true)
    public TaxTaskSubCategory getById(Integer id) {
        return taxTaskSubCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sub category not found"));
    }

    @Transactional(readOnly = true)
    public List<TaxRecordLookupResponse> getByCategory(Integer categoryId) {
        return taxTaskSubCategoryRepository.findByCategoryId(categoryId)
                .stream()
                .map(sc -> new TaxRecordLookupResponse(sc.getId(), sc.getName()))
                .toList();
    }

    @Transactional
    public TaxRecordLookupResponse create(Integer categoryId, String name) {
        TaxTaskCategory category = taxTaskCategoryService.getById(categoryId);

        if (taxTaskSubCategoryRepository.existsByNameAndCategoryId(name, categoryId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sub category already exists in this category");
        }

        TaxTaskSubCategory subCategory = new TaxTaskSubCategory();
        subCategory.setCategory(category);
        subCategory.setName(name);

        TaxTaskSubCategory saved = taxTaskSubCategoryRepository.save(subCategory);
        return new TaxRecordLookupResponse(saved.getId(), saved.getName());
    }

    @Transactional
    public void delete(Integer categoryId, Integer subCategoryId) {
        TaxTaskSubCategory subCategory = getById(subCategoryId);

        if (!subCategory.getCategory().getId().equals(categoryId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sub category not found in this category");
        }

        if (taxTaskNameRepository.existsBySubCategoryId(subCategoryId)
                || taxRecordTaskRepository.existsBySubCategoryId(subCategoryId)
                || taxRecordEntryRepository.existsBySubCategoryId(subCategoryId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sub category is referenced and cannot be deleted");
        }

        taxTaskSubCategoryRepository.delete(subCategory);
    }
}
