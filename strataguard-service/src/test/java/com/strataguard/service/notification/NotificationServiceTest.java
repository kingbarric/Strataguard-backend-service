package com.strataguard.service.notification;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.notification.*;
import com.strataguard.core.entity.Notification;
import com.strataguard.core.entity.Resident;
import com.strataguard.core.entity.Tenancy;
import com.strataguard.core.entity.Unit;
import com.strataguard.core.enums.NotificationChannel;
import com.strataguard.core.enums.NotificationStatus;
import com.strataguard.core.enums.NotificationType;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.NotificationMapper;
import com.strataguard.infrastructure.repository.NotificationRepository;
import com.strataguard.infrastructure.repository.ResidentRepository;
import com.strataguard.infrastructure.repository.TenancyRepository;
import com.strataguard.infrastructure.repository.UnitRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
class NotificationServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RESIDENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID RESIDENT_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID NOTIFICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID ESTATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000030");
    private static final UUID UNIT_ID = UUID.fromString("00000000-0000-0000-0000-000000000040");

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ResidentRepository residentRepository;

    @Mock
    private TenancyRepository tenancyRepository;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private NotificationPreferenceService preferenceService;

    @Mock
    private NotificationTemplateService templateService;

    @Mock
    private NotificationDispatcher dispatcher;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Resident buildResident(UUID id) {
        Resident resident = new Resident();
        resident.setId(id);
        resident.setTenantId(TENANT_ID);
        resident.setFirstName("John");
        resident.setLastName("Doe");
        resident.setEmail("john@example.com");
        resident.setPhone("+2341234567890");
        return resident;
    }

    private Notification buildNotification() {
        Notification notification = new Notification();
        notification.setId(NOTIFICATION_ID);
        notification.setTenantId(TENANT_ID);
        notification.setRecipientId(RESIDENT_ID);
        notification.setChannel(NotificationChannel.IN_APP);
        notification.setType(NotificationType.PAYMENT_RECEIVED);
        notification.setTitle("Payment Received");
        notification.setBody("Your payment of 5000 was received.");
        notification.setStatus(NotificationStatus.DELIVERED);
        notification.setCreatedAt(Instant.now());
        return notification;
    }

    private NotificationResponse buildNotificationResponse() {
        return NotificationResponse.builder()
                .id(NOTIFICATION_ID)
                .recipientId(RESIDENT_ID)
                .channel(NotificationChannel.IN_APP)
                .type(NotificationType.PAYMENT_RECEIVED)
                .title("Payment Received")
                .body("Your payment of 5000 was received.")
                .status(NotificationStatus.DELIVERED)
                .createdAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("send")
    class Send {

        @Test
        @DisplayName("should send notification to single recipient with specific channels")
        void shouldSendToSingleRecipientWithSpecificChannels() {
            SendNotificationRequest request = SendNotificationRequest.builder()
                    .recipientId(RESIDENT_ID)
                    .type(NotificationType.PAYMENT_RECEIVED)
                    .title("Payment Received")
                    .body("Your payment was received.")
                    .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                    .build();

            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(buildResident(RESIDENT_ID)));
            when(preferenceService.isChannelEnabled(eq(RESIDENT_ID), any(), eq(NotificationType.PAYMENT_RECEIVED)))
                    .thenReturn(true);
            when(templateService.resolveBody(any(), any(), any(), any())).thenReturn(null);
            when(templateService.resolveSubject(any(), any(), any(), any())).thenReturn(null);
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId(UUID.randomUUID());
                return n;
            });

            notificationService.send(request);

            // Should create 2 notifications (IN_APP + EMAIL)
            verify(notificationRepository, times(2)).save(any(Notification.class));
            verify(dispatcher, times(2)).dispatch(any(Notification.class));
        }

        @Test
        @DisplayName("should send notification to multiple recipients")
        void shouldSendToMultipleRecipients() {
            SendNotificationRequest request = SendNotificationRequest.builder()
                    .recipientIds(List.of(RESIDENT_ID, RESIDENT_ID_2))
                    .type(NotificationType.ANNOUNCEMENT)
                    .title("Announcement")
                    .body("Important announcement.")
                    .channels(List.of(NotificationChannel.IN_APP))
                    .build();

            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(buildResident(RESIDENT_ID)));
            when(residentRepository.findByIdAndTenantId(RESIDENT_ID_2, TENANT_ID))
                    .thenReturn(Optional.of(buildResident(RESIDENT_ID_2)));
            when(preferenceService.isChannelEnabled(any(), any(), any())).thenReturn(true);
            when(templateService.resolveBody(any(), any(), any(), any())).thenReturn(null);
            when(templateService.resolveSubject(any(), any(), any(), any())).thenReturn(null);
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId(UUID.randomUUID());
                return n;
            });

            notificationService.send(request);

            // 2 recipients x 1 channel = 2 notifications
            verify(notificationRepository, times(2)).save(any(Notification.class));
            verify(dispatcher, times(2)).dispatch(any(Notification.class));
        }

        @Test
        @DisplayName("should skip disabled channels based on preferences")
        void shouldSkipDisabledChannels() {
            SendNotificationRequest request = SendNotificationRequest.builder()
                    .recipientId(RESIDENT_ID)
                    .type(NotificationType.PAYMENT_DUE)
                    .title("Payment Due")
                    .body("Your payment is due.")
                    .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.SMS, NotificationChannel.EMAIL))
                    .build();

            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(buildResident(RESIDENT_ID)));
            when(preferenceService.isChannelEnabled(RESIDENT_ID, NotificationChannel.IN_APP, NotificationType.PAYMENT_DUE))
                    .thenReturn(true);
            when(preferenceService.isChannelEnabled(RESIDENT_ID, NotificationChannel.SMS, NotificationType.PAYMENT_DUE))
                    .thenReturn(false); // SMS disabled
            when(preferenceService.isChannelEnabled(RESIDENT_ID, NotificationChannel.EMAIL, NotificationType.PAYMENT_DUE))
                    .thenReturn(true);
            when(templateService.resolveBody(any(), any(), any(), any())).thenReturn(null);
            when(templateService.resolveSubject(any(), any(), any(), any())).thenReturn(null);
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId(UUID.randomUUID());
                return n;
            });

            notificationService.send(request);

            // IN_APP + EMAIL = 2, SMS skipped
            verify(notificationRepository, times(2)).save(any(Notification.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when recipient not found")
        void shouldThrowWhenRecipientNotFound() {
            SendNotificationRequest request = SendNotificationRequest.builder()
                    .recipientId(RESIDENT_ID)
                    .type(NotificationType.GENERAL)
                    .title("Test")
                    .body("Test body")
                    .channels(List.of(NotificationChannel.IN_APP))
                    .build();

            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.send(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Resident");

            verify(notificationRepository, never()).save(any());
            verify(dispatcher, never()).dispatch(any());
        }

        @Test
        @DisplayName("should use all channels when no channels specified")
        void shouldUseAllChannelsWhenNoneSpecified() {
            SendNotificationRequest request = SendNotificationRequest.builder()
                    .recipientId(RESIDENT_ID)
                    .type(NotificationType.GENERAL)
                    .title("Test")
                    .body("Test body")
                    .build(); // no channels specified

            when(residentRepository.findByIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(buildResident(RESIDENT_ID)));
            when(preferenceService.isChannelEnabled(any(), any(), any())).thenReturn(true);
            when(templateService.resolveBody(any(), any(), any(), any())).thenReturn(null);
            when(templateService.resolveSubject(any(), any(), any(), any())).thenReturn(null);
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId(UUID.randomUUID());
                return n;
            });

            notificationService.send(request);

            // All 5 channels: IN_APP, EMAIL, SMS, WHATSAPP, PUSH
            verify(notificationRepository, times(5)).save(any(Notification.class));
        }
    }

    @Nested
    @DisplayName("sendBulk")
    class SendBulk {

        @Test
        @DisplayName("should send to all residents in estate")
        void shouldSendToAllResidentsInEstate() {
            BulkNotificationRequest request = BulkNotificationRequest.builder()
                    .estateId(ESTATE_ID)
                    .type(NotificationType.ANNOUNCEMENT)
                    .title("Estate Announcement")
                    .body("Important update for all residents.")
                    .build();

            Unit unit = new Unit();
            unit.setId(UNIT_ID);
            unit.setEstateId(ESTATE_ID);
            unit.setTenantId(TENANT_ID);

            Tenancy tenancy1 = new Tenancy();
            tenancy1.setResidentId(RESIDENT_ID);
            Tenancy tenancy2 = new Tenancy();
            tenancy2.setResidentId(RESIDENT_ID_2);

            when(unitRepository.findByEstateIdAndTenantId(ESTATE_ID, TENANT_ID)).thenReturn(List.of(unit));
            when(tenancyRepository.findActiveByUnitIdAndTenantId(UNIT_ID, TENANT_ID))
                    .thenReturn(List.of(tenancy1, tenancy2));
            when(residentRepository.findByIdAndTenantId(any(), eq(TENANT_ID)))
                    .thenReturn(Optional.of(buildResident(RESIDENT_ID)));
            when(preferenceService.isChannelEnabled(any(), any(), any())).thenReturn(true);
            when(templateService.resolveBody(any(), any(), any(), any())).thenReturn(null);
            when(templateService.resolveSubject(any(), any(), any(), any())).thenReturn(null);
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId(UUID.randomUUID());
                return n;
            });

            notificationService.sendBulk(request);

            // 2 residents x 5 channels = 10 notifications
            verify(notificationRepository, times(10)).save(any(Notification.class));
        }

        @Test
        @DisplayName("should do nothing when no residents in estate")
        void shouldDoNothingWhenNoResidents() {
            BulkNotificationRequest request = BulkNotificationRequest.builder()
                    .estateId(ESTATE_ID)
                    .type(NotificationType.ANNOUNCEMENT)
                    .title("Announcement")
                    .body("Test")
                    .build();

            when(unitRepository.findByEstateIdAndTenantId(ESTATE_ID, TENANT_ID)).thenReturn(List.of());

            notificationService.sendBulk(request);

            verify(notificationRepository, never()).save(any());
            verify(dispatcher, never()).dispatch(any());
        }
    }

    @Nested
    @DisplayName("getMyNotifications")
    class GetMyNotifications {

        @Test
        @DisplayName("should return paginated notifications for resident")
        void shouldReturnPaginatedNotifications() {
            Pageable pageable = PageRequest.of(0, 10);
            Notification notification = buildNotification();
            NotificationResponse response = buildNotificationResponse();
            Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);

            when(notificationRepository.findByRecipientIdAndTenantId(RESIDENT_ID, TENANT_ID, pageable))
                    .thenReturn(page);
            when(notificationMapper.toResponse(notification)).thenReturn(response);

            PagedResponse<NotificationResponse> result = notificationService.getMyNotifications(RESIDENT_ID, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Payment Received");
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getPage()).isZero();
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();
        }

        @Test
        @DisplayName("should return empty page when no notifications")
        void shouldReturnEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Notification> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(notificationRepository.findByRecipientIdAndTenantId(RESIDENT_ID, TENANT_ID, pageable))
                    .thenReturn(emptyPage);

            PagedResponse<NotificationResponse> result = notificationService.getMyNotifications(RESIDENT_ID, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {

        @Test
        @DisplayName("should mark notification as read and set readAt")
        void shouldMarkAsRead() {
            Notification notification = buildNotification();
            notification.setStatus(NotificationStatus.DELIVERED);

            NotificationResponse response = buildNotificationResponse();
            response.setStatus(NotificationStatus.READ);

            when(notificationRepository.findByIdAndRecipientIdAndTenantId(NOTIFICATION_ID, RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(notification));
            when(notificationRepository.save(notification)).thenReturn(notification);
            when(notificationMapper.toResponse(notification)).thenReturn(response);

            NotificationResponse result = notificationService.markAsRead(NOTIFICATION_ID, RESIDENT_ID);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(NotificationStatus.READ);
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.READ);
            assertThat(notification.getReadAt()).isNotNull();

            verify(notificationRepository).save(notification);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when notification not found")
        void shouldThrowWhenNotFound() {
            when(notificationRepository.findByIdAndRecipientIdAndTenantId(NOTIFICATION_ID, RESIDENT_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.markAsRead(NOTIFICATION_ID, RESIDENT_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Notification");

            verify(notificationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("markAllAsRead")
    class MarkAllAsRead {

        @Test
        @DisplayName("should call repository bulk update")
        void shouldCallBulkUpdate() {
            when(notificationRepository.markAllAsReadByRecipientIdAndTenantId(RESIDENT_ID, TENANT_ID))
                    .thenReturn(5);

            notificationService.markAllAsRead(RESIDENT_ID);

            verify(notificationRepository).markAllAsReadByRecipientIdAndTenantId(RESIDENT_ID, TENANT_ID);
        }
    }

    @Nested
    @DisplayName("getUnreadCount")
    class GetUnreadCount {

        @Test
        @DisplayName("should return correct unread count")
        void shouldReturnUnreadCount() {
            when(notificationRepository.countByRecipientIdAndStatusAndTenantId(
                    RESIDENT_ID, NotificationStatus.DELIVERED, TENANT_ID))
                    .thenReturn(7L);

            UnreadCountResponse result = notificationService.getUnreadCount(RESIDENT_ID);

            assertThat(result).isNotNull();
            assertThat(result.getCount()).isEqualTo(7L);
        }

        @Test
        @DisplayName("should return zero when no unread notifications")
        void shouldReturnZero() {
            when(notificationRepository.countByRecipientIdAndStatusAndTenantId(
                    RESIDENT_ID, NotificationStatus.DELIVERED, TENANT_ID))
                    .thenReturn(0L);

            UnreadCountResponse result = notificationService.getUnreadCount(RESIDENT_ID);

            assertThat(result.getCount()).isZero();
        }
    }
}
