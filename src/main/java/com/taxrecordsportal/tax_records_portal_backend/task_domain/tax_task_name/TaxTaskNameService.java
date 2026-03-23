package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_name;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category.TaxTaskSubCategory;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category.TaxTaskSubCategoryService;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_name.dto.response.TaxTaskNameResponse;
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

    @Transactional(readOnly = true)
    public List<TaxTaskNameResponse> getAll() {
        return taxTaskNameRepository.findAll()
                .stream()
                .map(t -> new TaxTaskNameResponse(t.getId(), t.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaxTaskNameResponse> getBySubCategory(Integer subCategoryId) {
        return taxTaskNameRepository.findBySubCategoryId(subCategoryId)
                .stream()
                .map(taskName -> new TaxTaskNameResponse(taskName.getId(), taskName.getName()))
                .toList();
    }

    public TaxTaskNameResponse create(Integer subCategoryId, String name) {
        TaxTaskSubCategory subCategory = taxTaskSubCategoryService.getById(subCategoryId);

        if (taxTaskNameRepository.existsByNameAndSubCategoryId(name, subCategoryId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task name already exists in this sub category");
        }

        TaxTaskName taskName = new TaxTaskName();
        taskName.setSubCategory(subCategory);
        taskName.setName(name);

        TaxTaskName saved = taxTaskNameRepository.save(taskName);
        return new TaxTaskNameResponse(saved.getId(), saved.getName());
    }
}
