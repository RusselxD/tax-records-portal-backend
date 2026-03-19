package com.taxrecordsportal.tax_records_portal_backend.user_domain.role.mapper;

import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.Role;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.role.dto.RoleListItemResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleListItemResponse toRoleListItem(Role role);
}
