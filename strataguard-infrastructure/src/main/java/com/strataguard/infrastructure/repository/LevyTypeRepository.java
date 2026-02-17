package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.LevyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LevyTypeRepository extends JpaRepository<LevyType, UUID> {

    @Query("SELECT l FROM LevyType l WHERE l.tenantId = :tenantId AND l.deleted = false")
    Page<LevyType> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT l FROM LevyType l WHERE l.id = :id AND l.tenantId = :tenantId AND l.deleted = false")
    Optional<LevyType> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT l FROM LevyType l WHERE l.estateId = :estateId AND l.tenantId = :tenantId AND l.deleted = false")
    Page<LevyType> findByEstateIdAndTenantId(@Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM LevyType l " +
            "WHERE l.name = :name AND l.estateId = :estateId AND l.tenantId = :tenantId AND l.deleted = false")
    boolean existsByNameAndEstateIdAndTenantId(@Param("name") String name, @Param("estateId") UUID estateId, @Param("tenantId") UUID tenantId);

    @Query("SELECT l FROM LevyType l WHERE l.tenantId = :tenantId AND l.deleted = false " +
            "AND (LOWER(l.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(l.category) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<LevyType> search(@Param("tenantId") UUID tenantId, @Param("search") String search, Pageable pageable);
}
