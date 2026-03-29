package com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_category;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.tax_record_entry.TaxRecordEntryRepository;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.TaxRecordTaskRepository;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response.LookupHierarchyResponse;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_record_task.dto.response.TaxRecordLookupResponse;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_name.TaxTaskName;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_name.TaxTaskNameRepository;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category.TaxTaskSubCategory;
import com.taxrecordsportal.tax_records_portal_backend.task_domain.tax_task_sub_category.TaxTaskSubCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaxTaskCategoryService {

    private final TaxTaskCategoryRepository taxTaskCategoryRepository;
    private final TaxTaskSubCategoryRepository taxTaskSubCategoryRepository;
    private final TaxTaskNameRepository taxTaskNameRepository;
    private final TaxRecordTaskRepository taxRecordTaskRepository;
    private final TaxRecordEntryRepository taxRecordEntryRepository;

    @Transactional(readOnly = true)
    public List<TaxRecordLookupResponse> getAll() {
        return taxTaskCategoryRepository.findAll()
                .stream()
                .map(c -> new TaxRecordLookupResponse(c.getId(), c.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TaxTaskCategory getById(Integer id) {
        return taxTaskCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    @Transactional
    public TaxRecordLookupResponse create(String name) {
        if (taxTaskCategoryRepository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category already exists");
        }
        TaxTaskCategory category = new TaxTaskCategory();
        category.setName(name);
        TaxTaskCategory saved = taxTaskCategoryRepository.save(category);
        return new TaxRecordLookupResponse(saved.getId(), saved.getName());
    }

    @Transactional(readOnly = true)
    public List<LookupHierarchyResponse> getHierarchy() {
        List<TaxTaskCategory> categories = taxTaskCategoryRepository.findAll();
        List<TaxTaskSubCategory> allSubCategories = taxTaskSubCategoryRepository.findAll();
        List<TaxTaskName> allTaskNames = taxTaskNameRepository.findAll();

        Map<Integer, List<LookupHierarchyResponse.TaskNameItem>> namesBySubCat = allTaskNames.stream()
                .collect(Collectors.groupingBy(
                        tn -> tn.getSubCategory().getId(),
                        Collectors.mapping(tn -> new LookupHierarchyResponse.TaskNameItem(tn.getId(), tn.getName()),
                                Collectors.toList())));

        Map<Integer, List<LookupHierarchyResponse.SubCategoryItem>> subCatsByCat = allSubCategories.stream()
                .collect(Collectors.groupingBy(
                        sc -> sc.getCategory().getId(),
                        Collectors.mapping(sc -> new LookupHierarchyResponse.SubCategoryItem(
                                sc.getId(), sc.getName(),
                                namesBySubCat.getOrDefault(sc.getId(), List.of())),
                                Collectors.toList())));

        return categories.stream()
                .map(cat -> new LookupHierarchyResponse(cat.getId(), cat.getName(),
                        subCatsByCat.getOrDefault(cat.getId(), List.of())))
                .toList();
    }

    @Transactional
    public void delete(Integer id) {
        TaxTaskCategory category = getById(id);

        if (taxTaskSubCategoryRepository.existsByCategoryId(id)
                || taxRecordTaskRepository.existsByCategoryId(id)
                || taxRecordEntryRepository.existsByCategoryId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category is referenced and cannot be deleted");
        }

        taxTaskCategoryRepository.delete(category);
    }
}
