package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.NotificationPreference;
import com.strataguard.core.enums.NotificationChannel;
import com.strataguard.core.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    @Query("SELECT p FROM NotificationPreference p WHERE p.residentId = :residentId " +
            "AND p.tenantId = :tenantId AND p.deleted = false")
    List<NotificationPreference> findByResidentIdAndTenantId(@Param("residentId") UUID residentId,
                                                              @Param("tenantId") UUID tenantId);

    @Query("SELECT p FROM NotificationPreference p WHERE p.residentId = :residentId " +
            "AND p.channel = :channel AND p.notificationType = :notificationType " +
            "AND p.tenantId = :tenantId AND p.deleted = false")
    Optional<NotificationPreference> findByResidentIdAndChannelAndNotificationTypeAndTenantId(
            @Param("residentId") UUID residentId,
            @Param("channel") NotificationChannel channel,
            @Param("notificationType") NotificationType notificationType,
            @Param("tenantId") UUID tenantId);
}
