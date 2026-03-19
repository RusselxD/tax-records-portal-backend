package com.taxrecordsportal.tax_records_portal_backend.user_domain.employee_position;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.employee_position.dto.request.EmployeePositionCreateRequest;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.employee_position.dto.response.EmployeePositionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeePositionService {

    private final EmployeePositionRepository employeePositionRepository;

    @Transactional(readOnly = true)
    public List<EmployeePositionResponse> getAll() {
        return employeePositionRepository.findAll().stream()
                .map(p -> new EmployeePositionResponse(p.getId(), p.getName()))
                .toList();
    }

    @Transactional
    public EmployeePositionResponse create(EmployeePositionCreateRequest request) {
        if (employeePositionRepository.existsByName(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Position already exists.");
        }

        EmployeePosition position = new EmployeePosition();
        position.setName(request.name());
        EmployeePosition saved = employeePositionRepository.save(position);

        return new EmployeePositionResponse(saved.getId(), saved.getName());
    }
}
