package com.strataguard.service.billing;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.payment.WalletResponse;
import com.strataguard.core.dto.payment.WalletTransactionResponse;
import com.strataguard.core.entity.Resident;
import com.strataguard.core.entity.ResidentWallet;
import com.strataguard.core.entity.WalletTransaction;
import com.strataguard.core.enums.WalletTransactionType;
import com.strataguard.core.exception.InsufficientFundsException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.WalletMapper;
import com.strataguard.infrastructure.repository.ResidentRepository;
import com.strataguard.infrastructure.repository.ResidentWalletRepository;
import com.strataguard.infrastructure.repository.WalletTransactionRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RESIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID WALLET_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID REFERENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID TRANSACTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");

    @Mock
    private ResidentWalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository transactionRepository;

    @Mock
    private ResidentRepository residentRepository;

    @Mock
    private WalletMapper walletMapper;

    @InjectMocks
    private WalletService walletService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private ResidentWallet buildWallet(BigDecimal balance) {
        ResidentWallet wallet = new ResidentWallet();
        wallet.setId(WALLET_ID);
        wallet.setTenantId(TENANT_ID);
        wallet.setResidentId(RESIDENT_ID);
        wallet.setBalance(balance);
        wallet.setActive(true);
        return wallet;
    }

    private Resident buildResident() {
        Resident resident = new Resident();
        resident.setId(RESIDENT_ID);
        resident.setTenantId(TENANT_ID);
        resident.setFirstName("John");
        resident.setLastName("Doe");
        return resident;
    }

    private WalletTransaction buildTransaction(BigDecimal amount, WalletTransactionType type, BigDecimal balanceAfter) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setId(TRANSACTION_ID);
        transaction.setTenantId(TENANT_ID);
        transaction.setWalletId(WALLET_ID);
        transaction.setAmount(amount);
        transaction.setTransactionType(type);
        transaction.setReferenceId(REFERENCE_ID);
        transaction.setReferenceType("PAYMENT");
        transaction.setDescription("Test transaction");
        transaction.setBalanceAfter(balanceAfter);
        return transaction;
    }

    @Nested
    @DisplayName("getOrCreateWallet")
    class GetOrCreateWallet {

        @Test
        @DisplayName("should return existing wallet")
        void shouldReturnExistingWallet() {
            ResidentWallet existingWallet = buildWallet(new BigDecimal("500.00"));

            when(walletRepository.findByResidentIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(existingWallet));

            ResidentWallet result = walletService.getOrCreateWallet(RESIDENT_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(WALLET_ID);
            assertThat(result.getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
            verify(walletRepository, never()).save(any());
        }

        @Test
        @DisplayName("should create new wallet when not found")
        void shouldCreateNewWalletWhenNotFound() {
            ResidentWallet newWallet = buildWallet(BigDecimal.ZERO);

            when(walletRepository.findByResidentIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.empty());
            when(walletRepository.save(any(ResidentWallet.class))).thenReturn(newWallet);

            ResidentWallet result = walletService.getOrCreateWallet(RESIDENT_ID);

            assertThat(result).isNotNull();
            assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getResidentId()).isEqualTo(RESIDENT_ID);
            verify(walletRepository).save(any(ResidentWallet.class));
        }
    }

    @Nested
    @DisplayName("credit")
    class Credit {

        @Test
        @DisplayName("should add amount to wallet balance and record transaction")
        void shouldCreditWalletSuccessfully() {
            ResidentWallet existingWallet = buildWallet(new BigDecimal("200.00"));
            ResidentWallet savedWallet = buildWallet(new BigDecimal("700.00"));

            when(walletRepository.findByResidentIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(existingWallet));
            when(walletRepository.save(any(ResidentWallet.class))).thenReturn(savedWallet);
            when(transactionRepository.save(any(WalletTransaction.class)))
                    .thenReturn(buildTransaction(new BigDecimal("500.00"), WalletTransactionType.CREDIT_OVERPAYMENT, new BigDecimal("700.00")));

            ResidentWallet result = walletService.credit(
                    RESIDENT_ID,
                    new BigDecimal("500.00"),
                    WalletTransactionType.CREDIT_OVERPAYMENT,
                    REFERENCE_ID,
                    "PAYMENT",
                    "Overpayment credit"
            );

            assertThat(result).isNotNull();
            assertThat(result.getBalance()).isEqualByComparingTo(new BigDecimal("700.00"));

            verify(walletRepository).save(any(ResidentWallet.class));
            verify(transactionRepository).save(any(WalletTransaction.class));
        }

        @Test
        @DisplayName("should create wallet if not exists then credit")
        void shouldCreateWalletAndCredit() {
            ResidentWallet newWallet = buildWallet(BigDecimal.ZERO);
            ResidentWallet creditedWallet = buildWallet(new BigDecimal("300.00"));

            when(walletRepository.findByResidentIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.empty());
            when(walletRepository.save(any(ResidentWallet.class)))
                    .thenReturn(newWallet)
                    .thenReturn(creditedWallet);
            when(transactionRepository.save(any(WalletTransaction.class)))
                    .thenReturn(buildTransaction(new BigDecimal("300.00"), WalletTransactionType.CREDIT_OVERPAYMENT, new BigDecimal("300.00")));

            ResidentWallet result = walletService.credit(
                    RESIDENT_ID,
                    new BigDecimal("300.00"),
                    WalletTransactionType.CREDIT_OVERPAYMENT,
                    REFERENCE_ID,
                    "PAYMENT",
                    "New wallet credit"
            );

            assertThat(result).isNotNull();
            // save called twice: once for wallet creation, once for balance update
            verify(walletRepository, times(2)).save(any(ResidentWallet.class));
            verify(transactionRepository).save(any(WalletTransaction.class));
        }
    }

    @Nested
    @DisplayName("debit")
    class Debit {

        @Test
        @DisplayName("should subtract amount from wallet balance and record transaction")
        void shouldDebitWalletSuccessfully() {
            ResidentWallet existingWallet = buildWallet(new BigDecimal("1000.00"));
            ResidentWallet savedWallet = buildWallet(new BigDecimal("600.00"));

            when(walletRepository.findByResidentIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(existingWallet));
            when(walletRepository.save(any(ResidentWallet.class))).thenReturn(savedWallet);
            when(transactionRepository.save(any(WalletTransaction.class)))
                    .thenReturn(buildTransaction(new BigDecimal("400.00"), WalletTransactionType.DEBIT_INVOICE_PAYMENT, new BigDecimal("600.00")));

            ResidentWallet result = walletService.debit(
                    RESIDENT_ID,
                    new BigDecimal("400.00"),
                    WalletTransactionType.DEBIT_INVOICE_PAYMENT,
                    REFERENCE_ID,
                    "INVOICE",
                    "Payment applied to invoice"
            );

            assertThat(result).isNotNull();
            assertThat(result.getBalance()).isEqualByComparingTo(new BigDecimal("600.00"));

            verify(walletRepository).save(any(ResidentWallet.class));
            verify(transactionRepository).save(any(WalletTransaction.class));
        }

        @Test
        @DisplayName("should throw InsufficientFundsException when balance is insufficient")
        void shouldThrowWhenInsufficientFunds() {
            ResidentWallet wallet = buildWallet(new BigDecimal("100.00"));

            when(walletRepository.findByResidentIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(wallet));

            assertThatThrownBy(() -> walletService.debit(
                    RESIDENT_ID,
                    new BigDecimal("500.00"),
                    WalletTransactionType.DEBIT_INVOICE_PAYMENT,
                    REFERENCE_ID,
                    "INVOICE",
                    "Payment"
            ))
                    .isInstanceOf(InsufficientFundsException.class)
                    .hasMessageContaining("Insufficient wallet balance")
                    .hasMessageContaining("100")
                    .hasMessageContaining("500");

            verify(walletRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when wallet not found")
        void shouldThrowWhenWalletNotFound() {
            when(walletRepository.findByResidentIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> walletService.debit(
                    RESIDENT_ID,
                    new BigDecimal("100.00"),
                    WalletTransactionType.DEBIT_INVOICE_PAYMENT,
                    REFERENCE_ID,
                    "INVOICE",
                    "Payment"
            ))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Wallet");

            verify(walletRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getWallet")
    class GetWallet {

        @Test
        @DisplayName("should return wallet with resident name enrichment when wallet exists")
        void shouldReturnWalletWithEnrichment() {
            ResidentWallet wallet = buildWallet(new BigDecimal("750.00"));
            Resident resident = buildResident();

            WalletResponse expectedResponse = WalletResponse.builder()
                    .id(WALLET_ID)
                    .residentId(RESIDENT_ID)
                    .balance(new BigDecimal("750.00"))
                    .build();

            when(walletRepository.findByResidentIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(wallet));
            when(walletMapper.toResponse(wallet)).thenReturn(expectedResponse);
            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(resident));

            WalletResponse result = walletService.getWallet(RESIDENT_ID);

            assertThat(result).isNotNull();
            assertThat(result.getBalance()).isEqualByComparingTo(new BigDecimal("750.00"));
            assertThat(result.getResidentName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("should return zero-balance wallet when wallet does not exist")
        void shouldReturnZeroBalanceWalletWhenNotExists() {
            WalletResponse expectedResponse = WalletResponse.builder()
                    .residentId(RESIDENT_ID)
                    .balance(BigDecimal.ZERO)
                    .build();

            when(walletRepository.findByResidentIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.empty());
            when(walletMapper.toResponse(any(ResidentWallet.class))).thenReturn(expectedResponse);
            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(buildResident()));

            WalletResponse result = walletService.getWallet(RESIDENT_ID);

            assertThat(result).isNotNull();
            assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getResidentName()).isEqualTo("John Doe");
        }
    }

    @Nested
    @DisplayName("getTransactions")
    class GetTransactions {

        @Test
        @DisplayName("should return paged transactions for wallet")
        void shouldReturnPagedTransactions() {
            Pageable pageable = PageRequest.of(0, 10);
            ResidentWallet wallet = buildWallet(new BigDecimal("500.00"));
            WalletTransaction transaction = buildTransaction(
                    new BigDecimal("500.00"),
                    WalletTransactionType.CREDIT_OVERPAYMENT,
                    new BigDecimal("500.00")
            );

            WalletTransactionResponse transactionResponse = WalletTransactionResponse.builder()
                    .id(TRANSACTION_ID)
                    .walletId(WALLET_ID)
                    .amount(new BigDecimal("500.00"))
                    .transactionType(WalletTransactionType.CREDIT_OVERPAYMENT)
                    .referenceId(REFERENCE_ID)
                    .referenceType("PAYMENT")
                    .description("Test transaction")
                    .balanceAfter(new BigDecimal("500.00"))
                    .createdAt(Instant.now())
                    .build();

            Page<WalletTransaction> page = new PageImpl<>(List.of(transaction), pageable, 1);

            when(walletRepository.findByResidentIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(wallet));
            when(transactionRepository.findByWalletIdAndTenantId(WALLET_ID, TENANT_ID, pageable))
                    .thenReturn(page);
            when(walletMapper.toTransactionResponse(transaction)).thenReturn(transactionResponse);

            PagedResponse<WalletTransactionResponse> result = walletService.getTransactions(RESIDENT_ID, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(result.getContent().get(0).getTransactionType()).isEqualTo(WalletTransactionType.CREDIT_OVERPAYMENT);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getPage()).isZero();
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when wallet not found")
        void shouldThrowWhenWalletNotFound() {
            Pageable pageable = PageRequest.of(0, 10);

            when(walletRepository.findByResidentIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> walletService.getTransactions(RESIDENT_ID, pageable))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Wallet");
        }
    }
}
