package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_category.TaxTaskCategory;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_category.TaxTaskCategoryService;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category.dto.response.TaxTaskSubCategoryResponse;
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

    public TaxTaskSubCategory getById(Integer id) {
        return taxTaskSubCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sub category not found"));
    }

    @Transactional(readOnly = true)
    public List<TaxTaskSubCategoryResponse> getByCategory(Integer categoryId) {
        return taxTaskSubCategoryRepository.findByCategoryId(categoryId)
                .stream()
                .map(subCategory -> new TaxTaskSubCategoryResponse(subCategory.getId(), subCategory.getName()))
                .toList();
    }

    public TaxTaskSubCategoryResponse create(Integer categoryId, String name) {
        TaxTaskCategory category = taxTaskCategoryService.getById(categoryId);

        if (taxTaskSubCategoryRepository.existsByNameAndCategoryId(name, categoryId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sub category with this name already exists in this category");
        }

        TaxTaskSubCategory subCategory = new TaxTaskSubCategory();
        subCategory.setCategory(category);
        subCategory.setName(name);

        TaxTaskSubCategory saved = taxTaskSubCategoryRepository.save(subCategory);
        return new TaxTaskSubCategoryResponse(saved.getId(), saved.getName());
    }
}
