package com.taxrecordsportal.tax_records_portal_backend.user_domain.user.mapper;

import com.taxrecordsportal.tax_records_portal_backend.common.util.UserDisplayUtil;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.User;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response.AccountantListItemResponse;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response.ClientAccountResponse;
import com.taxrecordsportal.tax_records_portal_backend.user_domain.user.dto.response.UserListItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = UserDisplayUtil.class)
public interface UserMapper {

    @Mapping(target = "name", expression = "java(UserDisplayUtil.formatDisplayName(user))")
    @Mapping(target = "roleName", source = "role.name")
    @Mapping(target = "roleId", source = "role.id")
    @Mapping(target = "position", source = "position.name")
    @Mapping(target = "positionId", source = "position.id")
    UserListItemResponse mapUserToListItem(User user);

    ClientAccountResponse toClientAccountResponse(User user);

    @Mapping(target = "displayName", expression = "java(UserDisplayUtil.formatDisplayName(user))")
    @Mapping(target = "position", source = "position.name")
    @Mapping(target = "role", source = "role.name")
    @Mapping(target = "roleKey", source = "role.key")
    AccountantListItemResponse toAccountantListItemResponse(User user);
}
