package com.strataguard.service.billing;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.payment.WalletResponse;
import com.strataguard.core.dto.payment.WalletTransactionResponse;
import com.strataguard.core.entity.ResidentWallet;
import com.strataguard.core.entity.WalletTransaction;
import com.strataguard.core.enums.WalletTransactionType;
import com.strataguard.core.exception.InsufficientFundsException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.WalletMapper;
import com.strataguard.infrastructure.repository.ResidentRepository;
import com.strataguard.infrastructure.repository.ResidentWalletRepository;
import com.strataguard.infrastructure.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WalletService {

    private final ResidentWalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final ResidentRepository residentRepository;
    private final WalletMapper walletMapper;

    public ResidentWallet getOrCreateWallet(UUID residentId) {
        UUID tenantId = TenantContext.requireTenantId();
        return walletRepository.findByResidentIdAndTenantId(residentId, tenantId)
                .orElseGet(() -> {
                    ResidentWallet wallet = new ResidentWallet();
                    wallet.setTenantId(tenantId);
                    wallet.setResidentId(residentId);
                    wallet.setBalance(BigDecimal.ZERO);
                    ResidentWallet saved = walletRepository.save(wallet);
                    log.info("Created wallet for resident: {} tenant: {}", residentId, tenantId);
                    return saved;
                });
    }

    public ResidentWallet credit(UUID residentId, BigDecimal amount, WalletTransactionType type,
                                 UUID referenceId, String referenceType, String description) {
        UUID tenantId = TenantContext.requireTenantId();
        ResidentWallet wallet = getOrCreateWallet(residentId);

        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);
        wallet = walletRepository.save(wallet);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setTenantId(tenantId);
        transaction.setWalletId(wallet.getId());
        transaction.setAmount(amount);
        transaction.setTransactionType(type);
        transaction.setReferenceId(referenceId);
        transaction.setReferenceType(referenceType);
        transaction.setDescription(description);
        transaction.setBalanceAfter(newBalance);
        transactionRepository.save(transaction);

        log.info("Credited wallet {} with {} for resident: {}", wallet.getId(), amount, residentId);
        return wallet;
    }

    public ResidentWallet debit(UUID residentId, BigDecimal amount, WalletTransactionType type,
                                UUID referenceId, String referenceType, String description) {
        UUID tenantId = TenantContext.requireTenantId();
        ResidentWallet wallet = walletRepository.findByResidentIdAndTenantId(residentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "residentId", residentId));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    String.format("Insufficient wallet balance. Available: %s, Required: %s", wallet.getBalance(), amount));
        }

        BigDecimal newBalance = wallet.getBalance().subtract(amount);
        wallet.setBalance(newBalance);
        wallet = walletRepository.save(wallet);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setTenantId(tenantId);
        transaction.setWalletId(wallet.getId());
        transaction.setAmount(amount);
        transaction.setTransactionType(type);
        transaction.setReferenceId(referenceId);
        transaction.setReferenceType(referenceType);
        transaction.setDescription(description);
        transaction.setBalanceAfter(newBalance);
        transactionRepository.save(transaction);

        log.info("Debited wallet {} with {} for resident: {}", wallet.getId(), amount, residentId);
        return wallet;
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(UUID residentId) {
        UUID tenantId = TenantContext.requireTenantId();
        ResidentWallet wallet = walletRepository.findByResidentIdAndTenantId(residentId, tenantId)
                .orElseGet(() -> {
                    ResidentWallet w = new ResidentWallet();
                    w.setBalance(BigDecimal.ZERO);
                    w.setResidentId(residentId);
                    return w;
                });

        WalletResponse response = walletMapper.toResponse(wallet);
        residentRepository.findByIdAndTenantId(residentId, tenantId)
                .ifPresent(r -> response.setResidentName(r.getFirstName() + " " + r.getLastName()));
        return response;
    }

    @Transactional(readOnly = true)
    public PagedResponse<WalletTransactionResponse> getTransactions(UUID residentId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        ResidentWallet wallet = walletRepository.findByResidentIdAndTenantId(residentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "residentId", residentId));

        Page<WalletTransaction> page = transactionRepository.findByWalletIdAndTenantId(wallet.getId(), tenantId, pageable);
        return PagedResponse.<WalletTransactionResponse>builder()
                .content(page.getContent().stream().map(walletMapper::toTransactionResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
