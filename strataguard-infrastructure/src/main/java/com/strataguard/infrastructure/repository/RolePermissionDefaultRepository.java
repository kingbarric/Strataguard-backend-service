package com.strataguard.infrastructure.repository;

import com.strataguard.core.entity.RolePermissionDefault;
import com.strataguard.core.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RolePermissionDefaultRepository extends JpaRepository<RolePermissionDefault, UUID> {

    @Query("SELECT rpd.permission FROM RolePermissionDefault rpd WHERE rpd.role = :role")
    List<String> findPermissionsByRole(@Param("role") UserRole role);

    List<RolePermissionDefault> findAllByRole(UserRole role);

    void deleteAllByRole(UserRole role);

    void deleteByRoleAndPermission(UserRole role, String permission);

    boolean existsByRoleAndPermission(UserRole role, String permission);
}
