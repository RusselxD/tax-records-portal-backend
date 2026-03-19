package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_category;

import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_category.dto.response.TaxTaskCategoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaxTaskCategoryService {

    private final TaxTaskCategoryRepository taxTaskCategoryRepository;

    public List<TaxTaskCategoryResponse> getAll() {
        return taxTaskCategoryRepository.findAll()
                .stream()
                .map(category -> new TaxTaskCategoryResponse(category.getId(), category.getName()))
                .toList();
    }

    public TaxTaskCategory getById(Integer id) {
        return taxTaskCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tax task category not found"));
    }
}
