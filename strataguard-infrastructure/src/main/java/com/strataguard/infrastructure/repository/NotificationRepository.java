package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.Notification;
import com.strataguard.core.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId AND n.tenantId = :tenantId " +
            "AND n.channel = com.strataguard.core.enums.NotificationChannel.IN_APP AND n.deleted = false " +
            "ORDER BY n.createdAt DESC")
    Page<Notification> findByRecipientIdAndTenantId(@Param("recipientId") UUID recipientId,
                                                     @Param("tenantId") UUID tenantId,
                                                     Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId AND n.status = :status " +
            "AND n.tenantId = :tenantId AND n.channel = com.strataguard.core.enums.NotificationChannel.IN_APP " +
            "AND n.deleted = false")
    Page<Notification> findByRecipientIdAndStatusAndTenantId(@Param("recipientId") UUID recipientId,
                                                              @Param("status") NotificationStatus status,
                                                              @Param("tenantId") UUID tenantId,
                                                              Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipientId = :recipientId AND n.status = :status " +
            "AND n.tenantId = :tenantId AND n.channel = com.strataguard.core.enums.NotificationChannel.IN_APP " +
            "AND n.deleted = false")
    long countByRecipientIdAndStatusAndTenantId(@Param("recipientId") UUID recipientId,
                                                 @Param("status") NotificationStatus status,
                                                 @Param("tenantId") UUID tenantId);

    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.retryCount < :maxRetries " +
            "AND n.channel <> com.strataguard.core.enums.NotificationChannel.IN_APP AND n.deleted = false")
    List<Notification> findPendingForRetry(@Param("status") NotificationStatus status,
                                           @Param("maxRetries") int maxRetries);

    @Query("SELECT n FROM Notification n WHERE n.id = :id AND n.recipientId = :recipientId " +
            "AND n.tenantId = :tenantId AND n.deleted = false")
    Optional<Notification> findByIdAndRecipientIdAndTenantId(@Param("id") UUID id,
                                                              @Param("recipientId") UUID recipientId,
                                                              @Param("tenantId") UUID tenantId);

    @Modifying
    @Query("UPDATE Notification n SET n.status = com.strataguard.core.enums.NotificationStatus.READ, " +
            "n.readAt = CURRENT_TIMESTAMP WHERE n.recipientId = :recipientId AND n.tenantId = :tenantId " +
            "AND n.channel = com.strataguard.core.enums.NotificationChannel.IN_APP " +
            "AND n.status <> com.strataguard.core.enums.NotificationStatus.READ AND n.deleted = false")
    int markAllAsReadByRecipientIdAndTenantId(@Param("recipientId") UUID recipientId,
                                               @Param("tenantId") UUID tenantId);
}
