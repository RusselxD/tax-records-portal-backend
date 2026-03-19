package com.taxrecordsportal.tax_records_portal_backend.user_domain.seeder;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.employee_position.EmployeePosition;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.employee_position.EmployeePositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Order(1)
public class EmployeePositionSeeder implements CommandLineRunner {

    private final EmployeePositionRepository employeePositionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        List<String> positions = List.of(
                "Jr Accountant III",
                "Jr Accountant II",
                "Jr Accountant I",
                "Accounting Intern",
                "Operations MRE",
                "Sr Accountant I",
                "Sr Accountant II",
                "Sr Accountant III"
        );

        for (String name : positions) {
            if (!employeePositionRepository.existsByName(name)) {
                EmployeePosition position = new EmployeePosition();
                position.setName(name);
                employeePositionRepository.save(position);
            }
        }
    }
}
