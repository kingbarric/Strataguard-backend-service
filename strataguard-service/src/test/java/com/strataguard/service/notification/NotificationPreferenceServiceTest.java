package com.strataguard.service.notification;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.notification.NotificationPreferenceRequest;
import com.strataguard.core.dto.notification.NotificationPreferenceResponse;
import com.strataguard.core.entity.NotificationPreference;
import com.strataguard.core.enums.NotificationChannel;
import com.strataguard.core.enums.NotificationType;
import com.strataguard.core.util.NotificationPreferenceMapper;
import com.strataguard.infrastructure.repository.NotificationPreferenceRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RESIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID PREFERENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private NotificationPreferenceMapper preferenceMapper;

    @InjectMocks
    private NotificationPreferenceService preferenceService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private NotificationPreference buildPreference(NotificationChannel channel, NotificationType type, boolean enabled) {
        NotificationPreference pref = new NotificationPreference();
        pref.setId(PREFERENCE_ID);
        pref.setTenantId(TENANT_ID);
        pref.setResidentId(RESIDENT_ID);
        pref.setChannel(channel);
        pref.setNotificationType(type);
        pref.setEnabled(enabled);
        return pref;
    }

    private NotificationPreferenceResponse buildPreferenceResponse(NotificationChannel channel,
                                                                     NotificationType type, boolean enabled) {
        return NotificationPreferenceResponse.builder()
                .id(PREFERENCE_ID)
                .residentId(RESIDENT_ID)
                .channel(channel)
                .notificationType(type)
                .enabled(enabled)
                .build();
    }

    @Nested
    @DisplayName("getPreferences")
    class GetPreferences {

        @Test
        @DisplayName("should return all preferences for resident")
        void shouldReturnAllPreferences() {
            NotificationPreference pref = buildPreference(NotificationChannel.EMAIL, NotificationType.PAYMENT_RECEIVED, true);
            NotificationPreferenceResponse response = buildPreferenceResponse(NotificationChannel.EMAIL, NotificationType.PAYMENT_RECEIVED, true);

            when(preferenceRepository.findByResidentIdAndTenantId(RESIDENT_ID, TENANT_ID)).thenReturn(List.of(pref));
            when(preferenceMapper.toResponse(pref)).thenReturn(response);

            List<NotificationPreferenceResponse> result = preferenceService.getPreferences(RESIDENT_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getChannel()).isEqualTo(NotificationChannel.EMAIL);
            assertThat(result.get(0).isEnabled()).isTrue();

            verify(preferenceRepository).findByResidentIdAndTenantId(RESIDENT_ID, TENANT_ID);
        }

        @Test
        @DisplayName("should return empty list when no preferences exist")
        void shouldReturnEmptyList() {
            when(preferenceRepository.findByResidentIdAndTenantId(RESIDENT_ID, TENANT_ID)).thenReturn(List.of());

            List<NotificationPreferenceResponse> result = preferenceService.getPreferences(RESIDENT_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("updatePreference")
    class UpdatePreference {

        @Test
        @DisplayName("should update existing preference")
        void shouldUpdateExistingPreference() {
            NotificationPreferenceRequest request = NotificationPreferenceRequest.builder()
                    .channel(NotificationChannel.SMS)
                    .notificationType(NotificationType.PAYMENT_DUE)
                    .enabled(false)
                    .build();

            NotificationPreference existing = buildPreference(NotificationChannel.SMS, NotificationType.PAYMENT_DUE, true);
            NotificationPreferenceResponse response = buildPreferenceResponse(NotificationChannel.SMS, NotificationType.PAYMENT_DUE, false);

            when(preferenceRepository.findByResidentIdAndChannelAndNotificationTypeAndTenantId(
                    RESIDENT_ID, NotificationChannel.SMS, NotificationType.PAYMENT_DUE, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(preferenceRepository.save(existing)).thenReturn(existing);
            when(preferenceMapper.toResponse(existing)).thenReturn(response);

            NotificationPreferenceResponse result = preferenceService.updatePreference(RESIDENT_ID, request);

            assertThat(result).isNotNull();
            assertThat(result.isEnabled()).isFalse();
            assertThat(existing.isEnabled()).isFalse();

            verify(preferenceRepository).save(existing);
        }

        @Test
        @DisplayName("should create new preference when none exists")
        void shouldCreateNewPreference() {
            NotificationPreferenceRequest request = NotificationPreferenceRequest.builder()
                    .channel(NotificationChannel.WHATSAPP)
                    .notificationType(NotificationType.VISITOR_ARRIVED)
                    .enabled(false)
                    .build();

            NotificationPreferenceResponse response = buildPreferenceResponse(
                    NotificationChannel.WHATSAPP, NotificationType.VISITOR_ARRIVED, false);

            when(preferenceRepository.findByResidentIdAndChannelAndNotificationTypeAndTenantId(
                    RESIDENT_ID, NotificationChannel.WHATSAPP, NotificationType.VISITOR_ARRIVED, TENANT_ID))
                    .thenReturn(Optional.empty());
            when(preferenceRepository.save(any(NotificationPreference.class))).thenAnswer(invocation -> {
                NotificationPreference saved = invocation.getArgument(0);
                saved.setId(PREFERENCE_ID);
                return saved;
            });
            when(preferenceMapper.toResponse(any(NotificationPreference.class))).thenReturn(response);

            NotificationPreferenceResponse result = preferenceService.updatePreference(RESIDENT_ID, request);

            assertThat(result).isNotNull();
            assertThat(result.isEnabled()).isFalse();

            verify(preferenceRepository).save(any(NotificationPreference.class));
        }
    }

    @Nested
    @DisplayName("isChannelEnabled")
    class IsChannelEnabled {

        @Test
        @DisplayName("should always return true for IN_APP channel")
        void shouldAlwaysReturnTrueForInApp() {
            boolean result = preferenceService.isChannelEnabled(
                    RESIDENT_ID, NotificationChannel.IN_APP, NotificationType.PAYMENT_RECEIVED);

            assertThat(result).isTrue();

            // Should not even query the repository
            verify(preferenceRepository, never()).findByResidentIdAndChannelAndNotificationTypeAndTenantId(
                    any(), any(), any(), any());
        }

        @Test
        @DisplayName("should always return true for ANNOUNCEMENT type")
        void shouldAlwaysReturnTrueForAnnouncement() {
            boolean result = preferenceService.isChannelEnabled(
                    RESIDENT_ID, NotificationChannel.EMAIL, NotificationType.ANNOUNCEMENT);

            assertThat(result).isTrue();

            verify(preferenceRepository, never()).findByResidentIdAndChannelAndNotificationTypeAndTenantId(
                    any(), any(), any(), any());
        }

        @Test
        @DisplayName("should return preference value when preference exists")
        void shouldReturnPreferenceValue() {
            NotificationPreference disabledPref = buildPreference(
                    NotificationChannel.SMS, NotificationType.PAYMENT_DUE, false);

            when(preferenceRepository.findByResidentIdAndChannelAndNotificationTypeAndTenantId(
                    RESIDENT_ID, NotificationChannel.SMS, NotificationType.PAYMENT_DUE, TENANT_ID))
                    .thenReturn(Optional.of(disabledPref));

            boolean result = preferenceService.isChannelEnabled(
                    RESIDENT_ID, NotificationChannel.SMS, NotificationType.PAYMENT_DUE);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return true when preference enabled")
        void shouldReturnTrueWhenEnabled() {
            NotificationPreference enabledPref = buildPreference(
                    NotificationChannel.EMAIL, NotificationType.PAYMENT_RECEIVED, true);

            when(preferenceRepository.findByResidentIdAndChannelAndNotificationTypeAndTenantId(
                    RESIDENT_ID, NotificationChannel.EMAIL, NotificationType.PAYMENT_RECEIVED, TENANT_ID))
                    .thenReturn(Optional.of(enabledPref));

            boolean result = preferenceService.isChannelEnabled(
                    RESIDENT_ID, NotificationChannel.EMAIL, NotificationType.PAYMENT_RECEIVED);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should default to true when no preference record exists")
        void shouldDefaultToTrueWhenNoPreference() {
            when(preferenceRepository.findByResidentIdAndChannelAndNotificationTypeAndTenantId(
                    RESIDENT_ID, NotificationChannel.PUSH, NotificationType.GATE_ENTRY, TENANT_ID))
                    .thenReturn(Optional.empty());

            boolean result = preferenceService.isChannelEnabled(
                    RESIDENT_ID, NotificationChannel.PUSH, NotificationType.GATE_ENTRY);

            assertThat(result).isTrue();
        }
    }
}
