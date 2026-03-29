package com.taxrecordsportal.tax_records_portal_backend.notifications_domain.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndReadFalseOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    long countByRecipientIdAndReadFalse(UUID recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.id = :id AND n.recipient.id = :recipientId")
    int markAsReadByIdAndRecipientId(@Param("id") UUID id, @Param("recipientId") UUID recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipient.id = :recipientId AND n.read = false")
    int markAllReadByRecipientId(@Param("recipientId") UUID recipientId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.id = :id AND n.recipient.id = :recipientId")
    int deleteByIdAndRecipientId(@Param("id") UUID id, @Param("recipientId") UUID recipientId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.referenceId = :referenceId AND n.referenceType = :referenceType")
    void deleteAllByReferenceIdAndReferenceType(@Param("referenceId") UUID referenceId, @Param("referenceType") ReferenceType referenceType);
}
