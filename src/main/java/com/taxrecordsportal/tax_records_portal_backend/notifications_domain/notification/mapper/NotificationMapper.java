package com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.mapper;

import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.Notification;
import com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification.dto.response.NotificationListItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "isRead", source = "read")
    NotificationListItemResponse toListItem(Notification notification);
}
