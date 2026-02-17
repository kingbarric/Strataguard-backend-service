package com.strataguard.api.controller;

import com.strataguard.core.dto.blacklist.BlacklistResponse;
import com.strataguard.core.dto.blacklist.CreateBlacklistRequest;
import com.strataguard.core.dto.blacklist.UpdateBlacklistRequest;
import com.strataguard.core.dto.common.ApiResponse;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.service.visitor.BlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlacklistControllerTest {

    @Mock
    private BlacklistService blacklistService;

    @InjectMocks
    private BlacklistController blacklistController;

    private static final UUID BLACKLIST_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final Pageable PAGEABLE = PageRequest.of(0, 20);

    private BlacklistResponse blacklistResponse;
    private PagedResponse<BlacklistResponse> pagedResponse;

    @BeforeEach
    void setUp() {
        blacklistResponse = BlacklistResponse.builder()
                .id(BLACKLIST_ID)
                .name("John Doe")
                .phone("+254712345678")
                .plateNumber("KAA123A")
                .reason("Trespassing")
                .active(true)
                .addedBy("user-keycloak-id-001")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        pagedResponse = PagedResponse.<BlacklistResponse>builder()
                .content(List.of(blacklistResponse))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();
    }

    // ========================================================================
    // create
    // ========================================================================

    @Nested
    @DisplayName("POST / - create")
    class Create {

        private CreateBlacklistRequest createRequest;

        @BeforeEach
        void setUp() {
            createRequest = CreateBlacklistRequest.builder()
                    .name("John Doe")
                    .phone("+254712345678")
                    .plateNumber("KAA 123A")
                    .reason("Trespassing")
                    .build();
        }

        @Test
        @DisplayName("should return 201 CREATED with success message")
        void shouldReturnCreatedStatus() {
            when(blacklistService.create(createRequest)).thenReturn(blacklistResponse);

            ResponseEntity<ApiResponse<BlacklistResponse>> result =
                    blacklistController.create(createRequest);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Blacklist entry created successfully");
            assertThat(result.getBody().getData()).isEqualTo(blacklistResponse);
            assertThat(result.getBody().getData().getId()).isEqualTo(BLACKLIST_ID);

            verify(blacklistService).create(createRequest);
        }

        @Test
        @DisplayName("should delegate to blacklistService.create")
        void shouldDelegateToService() {
            when(blacklistService.create(createRequest)).thenReturn(blacklistResponse);

            blacklistController.create(createRequest);

            verify(blacklistService).create(createRequest);
        }
    }

    // ========================================================================
    // getAll
    // ========================================================================

    @Nested
    @DisplayName("GET / - getAll")
    class GetAll {

        @Test
        @DisplayName("should return 200 OK with paged blacklist entries")
        void shouldReturnOkWithPagedEntries() {
            when(blacklistService.getAll(PAGEABLE)).thenReturn(pagedResponse);

            ResponseEntity<ApiResponse<PagedResponse<BlacklistResponse>>> result =
                    blacklistController.getAll(PAGEABLE);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(pagedResponse);
            assertThat(result.getBody().getData().getContent()).hasSize(1);
            assertThat(result.getBody().getData().getTotalElements()).isEqualTo(1);

            verify(blacklistService).getAll(PAGEABLE);
        }

        @Test
        @DisplayName("should return 200 OK with empty page when no entries exist")
        void shouldReturnOkWithEmptyPage() {
            PagedResponse<BlacklistResponse> emptyPage = PagedResponse.<BlacklistResponse>builder()
                    .content(List.of())
                    .page(0)
                    .size(20)
                    .totalElements(0)
                    .totalPages(0)
                    .first(true)
                    .last(true)
                    .build();

            when(blacklistService.getAll(PAGEABLE)).thenReturn(emptyPage);

            ResponseEntity<ApiResponse<PagedResponse<BlacklistResponse>>> result =
                    blacklistController.getAll(PAGEABLE);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getData().getContent()).isEmpty();
            assertThat(result.getBody().getData().getTotalElements()).isZero();

            verify(blacklistService).getAll(PAGEABLE);
        }
    }

    // ========================================================================
    // getById
    // ========================================================================

    @Nested
    @DisplayName("GET /{id} - getById")
    class GetById {

        @Test
        @DisplayName("should return 200 OK with blacklist entry")
        void shouldReturnOkWithBlacklistEntry() {
            when(blacklistService.getById(BLACKLIST_ID)).thenReturn(blacklistResponse);

            ResponseEntity<ApiResponse<BlacklistResponse>> result =
                    blacklistController.getById(BLACKLIST_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(blacklistResponse);
            assertThat(result.getBody().getData().getId()).isEqualTo(BLACKLIST_ID);
            assertThat(result.getBody().getData().getName()).isEqualTo("John Doe");

            verify(blacklistService).getById(BLACKLIST_ID);
        }
    }

    // ========================================================================
    // update
    // ========================================================================

    @Nested
    @DisplayName("PUT /{id} - update")
    class Update {

        private UpdateBlacklistRequest updateRequest;

        @BeforeEach
        void setUp() {
            updateRequest = UpdateBlacklistRequest.builder()
                    .name("John Doe Updated")
                    .phone("+254799999999")
                    .plateNumber("KBB 456B")
                    .reason("Updated reason")
                    .active(true)
                    .build();
        }

        @Test
        @DisplayName("should return 200 OK with success message and updated entry")
        void shouldReturnOkWithUpdatedEntry() {
            BlacklistResponse updatedResponse = BlacklistResponse.builder()
                    .id(BLACKLIST_ID)
                    .name("John Doe Updated")
                    .phone("+254799999999")
                    .plateNumber("KBB456B")
                    .reason("Updated reason")
                    .active(true)
                    .addedBy("user-keycloak-id-001")
                    .build();

            when(blacklistService.update(BLACKLIST_ID, updateRequest)).thenReturn(updatedResponse);

            ResponseEntity<ApiResponse<BlacklistResponse>> result =
                    blacklistController.update(BLACKLIST_ID, updateRequest);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Blacklist entry updated successfully");
            assertThat(result.getBody().getData()).isEqualTo(updatedResponse);
            assertThat(result.getBody().getData().getName()).isEqualTo("John Doe Updated");

            verify(blacklistService).update(BLACKLIST_ID, updateRequest);
        }
    }

    // ========================================================================
    // deactivate
    // ========================================================================

    @Nested
    @DisplayName("POST /{id}/deactivate - deactivate")
    class Deactivate {

        @Test
        @DisplayName("should return 200 OK with success message and null data")
        void shouldReturnOkWithSuccessMessage() {
            ResponseEntity<ApiResponse<Void>> result = blacklistController.deactivate(BLACKLIST_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Blacklist entry deactivated successfully");
            assertThat(result.getBody().getData()).isNull();

            verify(blacklistService).deactivate(BLACKLIST_ID);
        }
    }

    // ========================================================================
    // delete
    // ========================================================================

    @Nested
    @DisplayName("DELETE /{id} - delete")
    class Delete {

        @Test
        @DisplayName("should return 200 OK with success message and null data")
        void shouldReturnOkWithSuccessMessage() {
            ResponseEntity<ApiResponse<Void>> result = blacklistController.delete(BLACKLIST_ID);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getMessage()).isEqualTo("Blacklist entry deleted successfully");
            assertThat(result.getBody().getData()).isNull();

            verify(blacklistService).delete(BLACKLIST_ID);
        }
    }

    // ========================================================================
    // search
    // ========================================================================

    @Nested
    @DisplayName("GET /search - search")
    class Search {

        @Test
        @DisplayName("should return 200 OK with paged search results")
        void shouldReturnOkWithSearchResults() {
            String query = "John";
            when(blacklistService.search(query, PAGEABLE)).thenReturn(pagedResponse);

            ResponseEntity<ApiResponse<PagedResponse<BlacklistResponse>>> result =
                    blacklistController.search(query, PAGEABLE);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEqualTo(pagedResponse);

            verify(blacklistService).search(query, PAGEABLE);
        }

        @Test
        @DisplayName("should return 200 OK with empty page when no results match")
        void shouldReturnOkWithEmptyResults() {
            String query = "NonExistent";
            PagedResponse<BlacklistResponse> emptyPage = PagedResponse.<BlacklistResponse>builder()
                    .content(List.of())
                    .page(0)
                    .size(20)
                    .totalElements(0)
                    .totalPages(0)
                    .first(true)
                    .last(true)
                    .build();

            when(blacklistService.search(query, PAGEABLE)).thenReturn(emptyPage);

            ResponseEntity<ApiResponse<PagedResponse<BlacklistResponse>>> result =
                    blacklistController.search(query, PAGEABLE);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getData().getContent()).isEmpty();
            assertThat(result.getBody().getData().getTotalElements()).isZero();

            verify(blacklistService).search(query, PAGEABLE);
        }
    }

    // ========================================================================
    // check
    // ========================================================================

    @Nested
    @DisplayName("GET /check - check")
    class Check {

        @Test
        @DisplayName("should return both phoneBlacklisted and plateBlacklisted when both params provided")
        void shouldReturnBothWhenBothParamsProvided() {
            String phone = "+254712345678";
            String plateNumber = "KAA123A";

            when(blacklistService.isPhoneBlacklisted(phone)).thenReturn(true);
            when(blacklistService.isPlateBlacklisted(plateNumber)).thenReturn(false);

            ResponseEntity<ApiResponse<Map<String, Boolean>>> result =
                    blacklistController.check(phone, plateNumber);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).containsEntry("phoneBlacklisted", true);
            assertThat(result.getBody().getData()).containsEntry("plateBlacklisted", false);
            assertThat(result.getBody().getData()).hasSize(2);

            verify(blacklistService).isPhoneBlacklisted(phone);
            verify(blacklistService).isPlateBlacklisted(plateNumber);
        }

        @Test
        @DisplayName("should return only phoneBlacklisted when only phone is provided")
        void shouldReturnOnlyPhoneWhenOnlyPhoneProvided() {
            String phone = "+254712345678";

            when(blacklistService.isPhoneBlacklisted(phone)).thenReturn(true);

            ResponseEntity<ApiResponse<Map<String, Boolean>>> result =
                    blacklistController.check(phone, null);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).containsEntry("phoneBlacklisted", true);
            assertThat(result.getBody().getData()).doesNotContainKey("plateBlacklisted");
            assertThat(result.getBody().getData()).hasSize(1);

            verify(blacklistService).isPhoneBlacklisted(phone);
        }

        @Test
        @DisplayName("should return only plateBlacklisted when only plateNumber is provided")
        void shouldReturnOnlyPlateWhenOnlyPlateProvided() {
            String plateNumber = "KAA123A";

            when(blacklistService.isPlateBlacklisted(plateNumber)).thenReturn(false);

            ResponseEntity<ApiResponse<Map<String, Boolean>>> result =
                    blacklistController.check(null, plateNumber);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).containsEntry("plateBlacklisted", false);
            assertThat(result.getBody().getData()).doesNotContainKey("phoneBlacklisted");
            assertThat(result.getBody().getData()).hasSize(1);

            verify(blacklistService).isPlateBlacklisted(plateNumber);
        }

        @Test
        @DisplayName("should return empty map when neither phone nor plateNumber is provided")
        void shouldReturnEmptyMapWhenNoParamsProvided() {
            ResponseEntity<ApiResponse<Map<String, Boolean>>> result =
                    blacklistController.check(null, null);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().isSuccess()).isTrue();
            assertThat(result.getBody().getData()).isEmpty();

            verifyNoInteractions(blacklistService);
        }

        @Test
        @DisplayName("should treat blank phone as absent and not call service")
        void shouldTreatBlankPhoneAsAbsent() {
            String plateNumber = "KAA123A";

            when(blacklistService.isPlateBlacklisted(plateNumber)).thenReturn(true);

            ResponseEntity<ApiResponse<Map<String, Boolean>>> result =
                    blacklistController.check("   ", plateNumber);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getData()).containsEntry("plateBlacklisted", true);
            assertThat(result.getBody().getData()).doesNotContainKey("phoneBlacklisted");
            assertThat(result.getBody().getData()).hasSize(1);

            verify(blacklistService).isPlateBlacklisted(plateNumber);
        }

        @Test
        @DisplayName("should treat blank plateNumber as absent and not call service")
        void shouldTreatBlankPlateAsAbsent() {
            String phone = "+254712345678";

            when(blacklistService.isPhoneBlacklisted(phone)).thenReturn(false);

            ResponseEntity<ApiResponse<Map<String, Boolean>>> result =
                    blacklistController.check(phone, "  ");

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isNotNull();
            assertThat(result.getBody().getData()).containsEntry("phoneBlacklisted", false);
            assertThat(result.getBody().getData()).doesNotContainKey("plateBlacklisted");
            assertThat(result.getBody().getData()).hasSize(1);

            verify(blacklistService).isPhoneBlacklisted(phone);
        }
    }
}
