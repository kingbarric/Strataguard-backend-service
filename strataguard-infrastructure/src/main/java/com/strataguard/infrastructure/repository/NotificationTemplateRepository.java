package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.NotificationTemplate;
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
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    @Query("SELECT t FROM NotificationTemplate t WHERE t.notificationType = :type " +
            "AND t.channel = :channel AND t.tenantId = :tenantId AND t.active = true AND t.deleted = false")
    Optional<NotificationTemplate> findByNotificationTypeAndChannelAndTenantId(
            @Param("type") NotificationType type,
            @Param("channel") NotificationChannel channel,
            @Param("tenantId") UUID tenantId);

    @Query("SELECT t FROM NotificationTemplate t WHERE t.notificationType = :type " +
            "AND t.channel IS NULL AND t.tenantId = :tenantId AND t.active = true AND t.deleted = false")
    Optional<NotificationTemplate> findByNotificationTypeAndChannelIsNullAndTenantId(
            @Param("type") NotificationType type,
            @Param("tenantId") UUID tenantId);

    @Query("SELECT t FROM NotificationTemplate t WHERE t.estateId = :estateId " +
            "AND t.tenantId = :tenantId AND t.active = true AND t.deleted = false")
    List<NotificationTemplate> findByEstateIdAndTenantId(@Param("estateId") UUID estateId,
                                                          @Param("tenantId") UUID tenantId);

    @Query("SELECT t FROM NotificationTemplate t WHERE t.tenantId = :tenantId AND t.deleted = false")
    List<NotificationTemplate> findAllByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM NotificationTemplate t " +
            "WHERE t.name = :name AND t.tenantId = :tenantId AND t.deleted = false")
    boolean existsByNameAndTenantId(@Param("name") String name, @Param("tenantId") UUID tenantId);

    @Query("SELECT t FROM NotificationTemplate t WHERE t.id = :id AND t.tenantId = :tenantId AND t.deleted = false")
    Optional<NotificationTemplate> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT t FROM NotificationTemplate t WHERE t.notificationType = :type " +
            "AND t.channel = :channel AND t.estateId = :estateId AND t.tenantId = :tenantId " +
            "AND t.active = true AND t.deleted = false")
    Optional<NotificationTemplate> findByNotificationTypeAndChannelAndEstateIdAndTenantId(
            @Param("type") NotificationType type,
            @Param("channel") NotificationChannel channel,
            @Param("estateId") UUID estateId,
            @Param("tenantId") UUID tenantId);

    @Query("SELECT t FROM NotificationTemplate t WHERE t.notificationType = :type " +
            "AND t.channel IS NULL AND t.estateId = :estateId AND t.tenantId = :tenantId " +
            "AND t.active = true AND t.deleted = false")
    Optional<NotificationTemplate> findByNotificationTypeAndChannelIsNullAndEstateIdAndTenantId(
            @Param("type") NotificationType type,
            @Param("estateId") UUID estateId,
            @Param("tenantId") UUID tenantId);
}
