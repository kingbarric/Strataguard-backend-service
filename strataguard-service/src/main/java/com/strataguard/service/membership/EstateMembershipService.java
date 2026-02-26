package com.strataguard.service.membership;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.membership.*;
import com.strataguard.core.entity.Estate;
import com.strataguard.core.entity.EstateMembership;
import com.strataguard.core.enums.MembershipStatus;
import com.strataguard.core.enums.UserRole;
import com.strataguard.core.exception.DuplicateResourceException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.infrastructure.repository.EstateMembershipRepository;
import com.strataguard.infrastructure.repository.EstateRepository;
import com.strataguard.service.permission.PermissionResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EstateMembershipService {

    private final EstateMembershipRepository membershipRepository;
    private final EstateRepository estateRepository;
    private final PermissionResolver permissionResolver;

    public EstateMembershipResponse createMembership(CreateMembershipRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        Estate estate = estateRepository.findByIdAndTenantId(request.getEstateId(), tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Estate", "id", request.getEstateId()));

        if (membershipRepository.existsActiveByUserIdAndEstateId(request.getUserId(), request.getEstateId())) {
            throw new DuplicateResourceException("EstateMembership", "userId+estateId",
                request.getUserId() + "+" + request.getEstateId());
        }

        UserRole role = UserRole.valueOf(request.getRole());

        EstateMembership membership = new EstateMembership();
        membership.setTenantId(tenantId);
        membership.setUserId(request.getUserId());
        membership.setEstateId(request.getEstateId());
        membership.setRole(role);
        membership.setStatus(MembershipStatus.ACTIVE);

        if (request.getCustomPermissionsGranted() != null) {
            membership.setCustomPermissionsGranted(
                request.getCustomPermissionsGranted().toArray(new String[0]));
        }
        if (request.getCustomPermissionsRevoked() != null) {
            membership.setCustomPermissionsRevoked(
                request.getCustomPermissionsRevoked().toArray(new String[0]));
        }

        EstateMembership saved = membershipRepository.save(membership);
        log.info("Created membership: user={} estate={} role={}",
            request.getUserId(), request.getEstateId(), role);

        return toResponse(saved, estate.getName());
    }

    public EstateMembershipResponse updateMembership(UUID membershipId, UpdateMembershipRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        EstateMembership membership = membershipRepository.findById(membershipId)
            .filter(m -> m.getTenantId().equals(tenantId) && !m.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("EstateMembership", "id", membershipId));

        if (request.getRole() != null) {
            membership.setRole(UserRole.valueOf(request.getRole()));
        }
        if (request.getStatus() != null) {
            membership.setStatus(MembershipStatus.valueOf(request.getStatus()));
        }
        if (request.getCustomPermissionsGranted() != null) {
            membership.setCustomPermissionsGranted(
                request.getCustomPermissionsGranted().toArray(new String[0]));
        }
        if (request.getCustomPermissionsRevoked() != null) {
            membership.setCustomPermissionsRevoked(
                request.getCustomPermissionsRevoked().toArray(new String[0]));
        }

        EstateMembership saved = membershipRepository.save(membership);
        Estate estate = estateRepository.findById(saved.getEstateId()).orElse(null);
        String estateName = estate != null ? estate.getName() : "";

        return toResponse(saved, estateName);
    }

    @Transactional(readOnly = true)
    public UserEstatesResponse getUserMemberships(String userId) {
        UUID tenantId = TenantContext.requireTenantId();
        List<EstateMembership> memberships =
            membershipRepository.findAllActiveByUserIdAndTenantId(userId, tenantId);

        List<EstateMembershipResponse> responses = memberships.stream()
            .map(m -> {
                Estate estate = estateRepository.findById(m.getEstateId()).orElse(null);
                return toResponse(m, estate != null ? estate.getName() : "");
            }).toList();

        return UserEstatesResponse.builder()
            .userId(userId)
            .memberships(responses)
            .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<EstateMembershipResponse> getEstateMembers(UUID estateId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<EstateMembership> page =
            membershipRepository.findAllByEstateIdAndTenantId(estateId, tenantId, pageable);

        return PagedResponse.<EstateMembershipResponse>builder()
            .content(page.getContent().stream().map(m -> {
                Estate estate = estateRepository.findById(m.getEstateId()).orElse(null);
                return toResponse(m, estate != null ? estate.getName() : "");
            }).toList())
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .last(page.isLast())
            .first(page.isFirst())
            .build();
    }

    public void revokeMembership(UUID membershipId) {
        UUID tenantId = TenantContext.requireTenantId();
        EstateMembership membership = membershipRepository.findById(membershipId)
            .filter(m -> m.getTenantId().equals(tenantId) && !m.isDeleted())
            .orElseThrow(() -> new ResourceNotFoundException("EstateMembership", "id", membershipId));

        membership.setStatus(MembershipStatus.REVOKED);
        membershipRepository.save(membership);
        log.info("Revoked membership: {}", membershipId);
    }

    private EstateMembershipResponse toResponse(EstateMembership m, String estateName) {
        Set<String> effectivePermissions = permissionResolver.resolvePermissions(m);
        return EstateMembershipResponse.builder()
            .id(m.getId())
            .userId(m.getUserId())
            .estateId(m.getEstateId())
            .estateName(estateName)
            .role(m.getRole().name())
            .status(m.getStatus().name())
            .displayName(m.getDisplayName())
            .effectivePermissions(effectivePermissions)
            .createdAt(m.getCreatedAt())
            .build();
    }
}
