package com.taxrecordsportal.tax_records_portal_backend.user_domain.role;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.dto.RoleListItemResponse;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Transactional(readOnly = true)
    public List<RoleListItemResponse> getEmployeeRoles() {
        return roleRepository.findAllByKeyNot(RoleKey.CLIENT)
                .stream()
                .map(roleMapper::toRoleListItem)
                .toList();
    }
}
