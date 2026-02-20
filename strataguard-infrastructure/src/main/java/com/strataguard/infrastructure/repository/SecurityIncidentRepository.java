package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.SecurityIncident;
import com.strataguard.core.enums.IncidentCategory;
import com.strataguard.core.enums.IncidentSeverity;
import com.strataguard.core.enums.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SecurityIncidentRepository extends JpaRepository<SecurityIncident, UUID> {

    @Query("SELECT i FROM SecurityIncident i WHERE i.id = :id AND i.tenantId = :tenantId AND i.deleted = false")
    Optional<SecurityIncident> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT i FROM SecurityIncident i WHERE i.tenantId = :tenantId AND i.deleted = false")
    Page<SecurityIncident> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT i FROM SecurityIncident i WHERE i.estateId = :estateId AND i.tenantId = :tenantId AND i.deleted = false")
    Page<SecurityIncident> findByEstateIdAndTenantId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT i FROM SecurityIncident i WHERE i.status = :status AND i.tenantId = :tenantId AND i.deleted = false")
    Page<SecurityIncident> findByStatusAndTenantId(@Param("status") IncidentStatus status, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT i FROM SecurityIncident i WHERE i.severity = :severity AND i.tenantId = :tenantId AND i.deleted = false")
    Page<SecurityIncident> findBySeverityAndTenantId(@Param("severity") IncidentSeverity severity, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT i FROM SecurityIncident i WHERE i.tenantId = :tenantId AND i.deleted = false " +
            "AND (LOWER(i.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(i.description) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(i.incidentNumber) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<SecurityIncident> search(@Param("tenantId") UUID tenantId, @Param("query") String query, Pageable pageable);

    @Query("SELECT COUNT(i) FROM SecurityIncident i WHERE i.status = :status AND i.tenantId = :tenantId AND i.deleted = false")
    long countByStatusAndTenantId(@Param("status") IncidentStatus status, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(i) FROM SecurityIncident i WHERE i.severity = :severity AND i.tenantId = :tenantId AND i.deleted = false " +
            "AND i.status NOT IN (com.strataguard.core.enums.IncidentStatus.RESOLVED, com.strataguard.core.enums.IncidentStatus.CLOSED)")
    long countOpenBySeverityAndTenantId(@Param("severity") IncidentSeverity severity, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(i) FROM SecurityIncident i WHERE i.tenantId = :tenantId AND i.deleted = false " +
            "AND i.status NOT IN (com.strataguard.core.enums.IncidentStatus.RESOLVED, com.strataguard.core.enums.IncidentStatus.CLOSED)")
    long countOpenByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(i) FROM SecurityIncident i WHERE i.tenantId = :tenantId AND i.deleted = false " +
            "AND i.incidentNumber LIKE :prefix")
    long countByIncidentNumberPrefix(@Param("tenantId") UUID tenantId, @Param("prefix") String prefix);

    @Query("SELECT i FROM SecurityIncident i WHERE i.tenantId = :tenantId AND i.deleted = false ORDER BY i.createdAt DESC")
    Page<SecurityIncident> findRecentByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);
}
