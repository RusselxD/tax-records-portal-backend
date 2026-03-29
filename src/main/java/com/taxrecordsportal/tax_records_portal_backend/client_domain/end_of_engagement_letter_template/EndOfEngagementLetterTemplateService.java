package com.taxrecordsportal.tax_records_portal_backend.client_domain.end_of_engagement_letter_template;

import com.taxrecordsportal.tax_records_portal_backend.client_domain.end_of_engagement_letter_template.dto.request.CreateEndOfEngagementLetterTemplateRequest;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.end_of_engagement_letter_template.dto.request.UpdateEndOfEngagementLetterTemplateRequest;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.end_of_engagement_letter_template.dto.response.EndOfEngagementLetterTemplateListItemResponse;
import com.taxrecordsportal.tax_records_portal_backend.client_domain.end_of_engagement_letter_template.dto.response.EndOfEngagementLetterTemplateResponse;
import com.taxrecordsportal.tax_records_portal_backend.common.util.UserDisplayUtil;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static com.taxrecordsportal.tax_records_portal_backend.common.util.SecurityUtil.getCurrentUser;

@Service
@RequiredArgsConstructor
public class EndOfEngagementLetterTemplateService {

    private final EndOfEngagementLetterTemplateRepository repository;

    @Transactional(readOnly = true)
    public List<EndOfEngagementLetterTemplateListItemResponse> getAll() {
        return repository.findAll().stream()
                .map(t -> new EndOfEngagementLetterTemplateListItemResponse(
                        t.getId(), t.getName(),
                        UserDisplayUtil.formatDisplayName(t.getCreatedBy()),
                        t.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public EndOfEngagementLetterTemplateResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public EndOfEngagementLetterTemplateResponse create(CreateEndOfEngagementLetterTemplateRequest request) {
        if (repository.existsByName(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Template with this name already exists");
        }

        User currentUser = getCurrentUser();
        EndOfEngagementLetterTemplate template = new EndOfEngagementLetterTemplate();
        template.setName(request.name());
        template.setBody(request.body());
        template.setCreatedBy(currentUser);

        return toResponse(repository.save(template));
    }

    @Transactional
    public EndOfEngagementLetterTemplateResponse update(UUID id, UpdateEndOfEngagementLetterTemplateRequest request) {
        EndOfEngagementLetterTemplate template = findOrThrow(id);

        if (repository.existsByNameAndIdNot(request.name(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Template with this name already exists");
        }

        template.setName(request.name());
        template.setBody(request.body());

        return toResponse(repository.save(template));
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(findOrThrow(id));
    }

    public EndOfEngagementLetterTemplate findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));
    }

    private EndOfEngagementLetterTemplateResponse toResponse(EndOfEngagementLetterTemplate t) {
        return new EndOfEngagementLetterTemplateResponse(
                t.getId(), t.getName(), t.getBody(),
                UserDisplayUtil.formatDisplayName(t.getCreatedBy()),
                t.getCreatedAt(), t.getUpdatedAt());
    }
}
