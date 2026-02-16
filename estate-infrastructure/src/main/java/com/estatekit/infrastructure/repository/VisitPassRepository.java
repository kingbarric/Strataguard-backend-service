package com.estatekit.infrastructure.repository;

import com.estatekit.core.entity.VisitPass;
import com.estatekit.core.enums.VisitPassStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VisitPassRepository extends JpaRepository<VisitPass, UUID> {

    @Query("SELECT p FROM VisitPass p WHERE p.id = :id AND p.tenantId = :tenantId AND p.deleted = false")
    Optional<VisitPass> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT p FROM VisitPass p WHERE p.visitorId = :visitorId AND p.tenantId = :tenantId AND p.deleted = false")
    List<VisitPass> findByVisitorIdAndTenantId(@Param("visitorId") UUID visitorId,
                                               @Param("tenantId") UUID tenantId);

    @Query("SELECT p FROM VisitPass p WHERE p.passCode = :passCode AND p.deleted = false")
    Optional<VisitPass> findByPassCode(@Param("passCode") String passCode);

    @Query("SELECT p FROM VisitPass p WHERE p.verificationCode = :verificationCode " +
            "AND p.status = 'ACTIVE' AND p.tenantId = :tenantId AND p.deleted = false")
    Optional<VisitPass> findByVerificationCodeAndTenantId(@Param("verificationCode") String verificationCode,
                                                          @Param("tenantId") UUID tenantId);

    @Query("SELECT p FROM VisitPass p WHERE p.visitorId = :visitorId AND p.status = 'ACTIVE' " +
            "AND p.tenantId = :tenantId AND p.deleted = false")
    Optional<VisitPass> findActiveByVisitorIdAndTenantId(@Param("visitorId") UUID visitorId,
                                                         @Param("tenantId") UUID tenantId);

    @Query("SELECT p FROM VisitPass p WHERE p.token = :token AND p.deleted = false")
    Optional<VisitPass> findByToken(@Param("token") String token);
}
