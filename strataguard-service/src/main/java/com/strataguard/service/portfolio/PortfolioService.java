package com.strataguard.service.portfolio;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.portfolio.*;
import com.strataguard.core.entity.Estate;
import com.strataguard.core.entity.Portfolio;
import com.strataguard.core.entity.PortfolioMembership;
import com.strataguard.core.enums.MembershipStatus;
import com.strataguard.core.enums.UserRole;
import com.strataguard.core.exception.DuplicateResourceException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.infrastructure.repository.EstateRepository;
import com.strataguard.infrastructure.repository.PortfolioMembershipRepository;
import com.strataguard.infrastructure.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioMembershipRepository portfolioMembershipRepository;
    private final EstateRepository estateRepository;

    @Transactional
    public PortfolioResponse createPortfolio(CreatePortfolioRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        if (portfolioRepository.existsByNameAndTenantId(request.getName(), tenantId)) {
            throw new DuplicateResourceException("Portfolio with name '" + request.getName() + "' already exists");
        }

        Portfolio portfolio = new Portfolio();
        portfolio.setTenantId(tenantId);
        portfolio.setName(request.getName());
        portfolio.setDescription(request.getDescription());
        portfolio.setLogoUrl(request.getLogoUrl());
        portfolio.setContactEmail(request.getContactEmail());
        portfolio.setContactPhone(request.getContactPhone());

        portfolio = portfolioRepository.save(portfolio);
        log.info("Created portfolio: {} ({})", portfolio.getName(), portfolio.getId());

        return toResponse(portfolio);
    }

    @Transactional
    public PortfolioResponse updatePortfolio(UUID id, UpdatePortfolioRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        Portfolio portfolio = portfolioRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + id));

        if (request.getName() != null) {
            if (!request.getName().equals(portfolio.getName())
                    && portfolioRepository.existsByNameAndTenantId(request.getName(), tenantId)) {
                throw new DuplicateResourceException("Portfolio with name '" + request.getName() + "' already exists");
            }
            portfolio.setName(request.getName());
        }
        if (request.getDescription() != null) portfolio.setDescription(request.getDescription());
        if (request.getLogoUrl() != null) portfolio.setLogoUrl(request.getLogoUrl());
        if (request.getContactEmail() != null) portfolio.setContactEmail(request.getContactEmail());
        if (request.getContactPhone() != null) portfolio.setContactPhone(request.getContactPhone());
        if (request.getActive() != null) portfolio.setActive(request.getActive());

        portfolio = portfolioRepository.save(portfolio);
        log.info("Updated portfolio: {}", portfolio.getId());

        return toResponse(portfolio);
    }

    public PortfolioResponse getPortfolio(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        Portfolio portfolio = portfolioRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + id));
        return toResponse(portfolio);
    }

    public PagedResponse<PortfolioResponse> listPortfolios(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Portfolio> page = portfolioRepository.findAllByTenantId(tenantId, pageable);

        return PagedResponse.<PortfolioResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }

    @Transactional
    public void deletePortfolio(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        Portfolio portfolio = portfolioRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + id));
        portfolio.setDeleted(true);
        portfolioRepository.save(portfolio);
        log.info("Soft-deleted portfolio: {}", id);
    }

    @Transactional
    public void assignEstateToPortfolio(UUID portfolioId, UUID estateId) {
        UUID tenantId = TenantContext.requireTenantId();

        portfolioRepository.findByIdAndTenantId(portfolioId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + portfolioId));

        Estate estate = estateRepository.findByIdAndTenantId(estateId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Estate not found: " + estateId));

        estate.setPortfolioId(portfolioId);
        estateRepository.save(estate);
        log.info("Assigned estate {} to portfolio {}", estateId, portfolioId);
    }

    @Transactional
    public void removeEstateFromPortfolio(UUID portfolioId, UUID estateId) {
        UUID tenantId = TenantContext.requireTenantId();

        Estate estate = estateRepository.findByIdAndTenantId(estateId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Estate not found: " + estateId));

        if (!portfolioId.equals(estate.getPortfolioId())) {
            throw new IllegalStateException("Estate " + estateId + " is not assigned to portfolio " + portfolioId);
        }

        estate.setPortfolioId(null);
        estateRepository.save(estate);
        log.info("Removed estate {} from portfolio {}", estateId, portfolioId);
    }

    // ── Portfolio Membership ──

    @Transactional
    public PortfolioMembershipResponse addMember(UUID portfolioId, AddPortfolioMemberRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        portfolioRepository.findByIdAndTenantId(portfolioId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + portfolioId));

        UserRole role = parsePortfolioRole(request.getRole());

        if (portfolioMembershipRepository.existsActiveByUserIdAndPortfolioId(request.getUserId(), portfolioId)) {
            throw new DuplicateResourceException("User already has an active membership in this portfolio");
        }

        PortfolioMembership membership = new PortfolioMembership();
        membership.setTenantId(tenantId);
        membership.setUserId(request.getUserId());
        membership.setPortfolioId(portfolioId);
        membership.setRole(role);

        membership = portfolioMembershipRepository.save(membership);
        log.info("Added member {} to portfolio {} with role {}", request.getUserId(), portfolioId, role);

        return toMembershipResponse(membership, null);
    }

    public PagedResponse<PortfolioMembershipResponse> getMembers(UUID portfolioId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();

        Portfolio portfolio = portfolioRepository.findByIdAndTenantId(portfolioId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + portfolioId));

        Page<PortfolioMembership> page = portfolioMembershipRepository
                .findAllByPortfolioIdAndTenantId(portfolioId, tenantId, pageable);

        return PagedResponse.<PortfolioMembershipResponse>builder()
                .content(page.getContent().stream()
                        .map(pm -> toMembershipResponse(pm, portfolio.getName())).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }

    @Transactional
    public void removeMember(UUID portfolioId, UUID membershipId) {
        UUID tenantId = TenantContext.requireTenantId();

        PortfolioMembership membership = portfolioMembershipRepository.findById(membershipId)
                .filter(pm -> pm.getPortfolioId().equals(portfolioId) && pm.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio membership not found: " + membershipId));

        membership.setStatus(MembershipStatus.REVOKED);
        membership.setDeleted(true);
        portfolioMembershipRepository.save(membership);
        log.info("Removed member {} from portfolio {}", membership.getUserId(), portfolioId);
    }

    public List<PortfolioMembershipResponse> getMyPortfolios(String userId) {
        return portfolioMembershipRepository.findAllActiveByUserId(userId).stream()
                .map(pm -> {
                    String portfolioName = portfolioRepository.findById(pm.getPortfolioId())
                            .map(Portfolio::getName).orElse(null);
                    return toMembershipResponse(pm, portfolioName);
                })
                .toList();
    }

    // ── Helpers ──

    private PortfolioResponse toResponse(Portfolio portfolio) {
        UUID tenantId = TenantContext.requireTenantId();
        long estateCount = estateRepository.findAllByTenantId(tenantId, Pageable.unpaged())
                .getContent().stream()
                .filter(e -> portfolio.getId().equals(e.getPortfolioId()))
                .count();

        return PortfolioResponse.builder()
                .id(portfolio.getId())
                .name(portfolio.getName())
                .description(portfolio.getDescription())
                .logoUrl(portfolio.getLogoUrl())
                .contactEmail(portfolio.getContactEmail())
                .contactPhone(portfolio.getContactPhone())
                .active(portfolio.isActive())
                .estateCount((int) estateCount)
                .createdAt(portfolio.getCreatedAt())
                .updatedAt(portfolio.getUpdatedAt())
                .build();
    }

    private PortfolioMembershipResponse toMembershipResponse(PortfolioMembership pm, String portfolioName) {
        return PortfolioMembershipResponse.builder()
                .id(pm.getId())
                .userId(pm.getUserId())
                .portfolioId(pm.getPortfolioId())
                .portfolioName(portfolioName)
                .role(pm.getRole())
                .status(pm.getStatus())
                .createdAt(pm.getCreatedAt())
                .build();
    }

    private UserRole parsePortfolioRole(String role) {
        UserRole userRole = UserRole.valueOf(role);
        if (userRole != UserRole.PORTFOLIO_ADMIN && userRole != UserRole.PORTFOLIO_VIEWER) {
            throw new IllegalArgumentException("Invalid portfolio role: " + role
                    + ". Must be PORTFOLIO_ADMIN or PORTFOLIO_VIEWER");
        }
        return userRole;
    }
}
