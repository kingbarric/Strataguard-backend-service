package com.strataguard.service.notification;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.notification.NotificationTemplateRequest;
import com.strataguard.core.dto.notification.NotificationTemplateResponse;
import com.strataguard.core.entity.NotificationTemplate;
import com.strataguard.core.enums.NotificationChannel;
import com.strataguard.core.enums.NotificationType;
import com.strataguard.core.exception.DuplicateResourceException;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.NotificationTemplateMapper;
import com.strataguard.infrastructure.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationTemplateServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID TEMPLATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID ESTATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

    @Mock
    private NotificationTemplateRepository templateRepository;

    @Mock
    private NotificationTemplateMapper templateMapper;

    @InjectMocks
    private NotificationTemplateService templateService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private NotificationTemplate buildTemplate() {
        NotificationTemplate template = new NotificationTemplate();
        template.setId(TEMPLATE_ID);
        template.setTenantId(TENANT_ID);
        template.setName("Payment Received");
        template.setNotificationType(NotificationType.PAYMENT_RECEIVED);
        template.setChannel(NotificationChannel.EMAIL);
        template.setSubjectTemplate("Payment Confirmation for {{amount}}");
        template.setBodyTemplate("Hello {{firstName}}, your payment of {{amount}} was received.");
        template.setActive(true);
        template.setCreatedAt(Instant.now());
        return template;
    }

    private NotificationTemplateResponse buildTemplateResponse() {
        return NotificationTemplateResponse.builder()
                .id(TEMPLATE_ID)
                .name("Payment Received")
                .notificationType(NotificationType.PAYMENT_RECEIVED)
                .channel(NotificationChannel.EMAIL)
                .subjectTemplate("Payment Confirmation for {{amount}}")
                .bodyTemplate("Hello {{firstName}}, your payment of {{amount}} was received.")
                .active(true)
                .createdAt(Instant.now())
                .build();
    }

    private NotificationTemplateRequest buildTemplateRequest() {
        return NotificationTemplateRequest.builder()
                .name("Payment Received")
                .notificationType(NotificationType.PAYMENT_RECEIVED)
                .channel(NotificationChannel.EMAIL)
                .subjectTemplate("Payment Confirmation for {{amount}}")
                .bodyTemplate("Hello {{firstName}}, your payment of {{amount}} was received.")
                .build();
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create template successfully")
        void shouldCreateTemplateSuccessfully() {
            NotificationTemplateRequest request = buildTemplateRequest();
            NotificationTemplate template = buildTemplate();
            NotificationTemplateResponse response = buildTemplateResponse();

            when(templateRepository.existsByNameAndTenantId("Payment Received", TENANT_ID)).thenReturn(false);
            when(templateMapper.toEntity(request)).thenReturn(template);
            when(templateRepository.save(template)).thenReturn(template);
            when(templateMapper.toResponse(template)).thenReturn(response);

            NotificationTemplateResponse result = templateService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(TEMPLATE_ID);
            assertThat(result.getName()).isEqualTo("Payment Received");
            assertThat(result.getNotificationType()).isEqualTo(NotificationType.PAYMENT_RECEIVED);
            assertThat(result.isActive()).isTrue();

            verify(templateRepository).existsByNameAndTenantId("Payment Received", TENANT_ID);
            verify(templateMapper).toEntity(request);
            verify(templateRepository).save(template);
        }

        @Test
        @DisplayName("should set tenant ID on created template")
        void shouldSetTenantId() {
            NotificationTemplateRequest request = buildTemplateRequest();
            NotificationTemplate template = buildTemplate();
            template.setTenantId(null);

            when(templateRepository.existsByNameAndTenantId("Payment Received", TENANT_ID)).thenReturn(false);
            when(templateMapper.toEntity(request)).thenReturn(template);
            when(templateRepository.save(template)).thenReturn(template);
            when(templateMapper.toResponse(template)).thenReturn(buildTemplateResponse());

            templateService.create(request);

            assertThat(template.getTenantId()).isEqualTo(TENANT_ID);
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when name already exists")
        void shouldThrowWhenNameExists() {
            NotificationTemplateRequest request = buildTemplateRequest();

            when(templateRepository.existsByNameAndTenantId("Payment Received", TENANT_ID)).thenReturn(true);

            assertThatThrownBy(() -> templateService.create(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("NotificationTemplate")
                    .hasMessageContaining("name");

            verify(templateRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getAll")
    class GetAll {

        @Test
        @DisplayName("should return all templates for tenant")
        void shouldReturnAllTemplates() {
            NotificationTemplate template = buildTemplate();
            NotificationTemplateResponse response = buildTemplateResponse();

            when(templateRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(template));
            when(templateMapper.toResponse(template)).thenReturn(response);

            List<NotificationTemplateResponse> result = templateService.getAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Payment Received");

            verify(templateRepository).findAllByTenantId(TENANT_ID);
        }

        @Test
        @DisplayName("should return empty list when no templates exist")
        void shouldReturnEmptyList() {
            when(templateRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of());

            List<NotificationTemplateResponse> result = templateService.getAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("should return template when found")
        void shouldReturnTemplateWhenFound() {
            NotificationTemplate template = buildTemplate();
            NotificationTemplateResponse response = buildTemplateResponse();

            when(templateRepository.findByIdAndTenantId(TEMPLATE_ID, TENANT_ID)).thenReturn(Optional.of(template));
            when(templateMapper.toResponse(template)).thenReturn(response);

            NotificationTemplateResponse result = templateService.getById(TEMPLATE_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(TEMPLATE_ID);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(templateRepository.findByIdAndTenantId(TEMPLATE_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> templateService.getById(TEMPLATE_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("NotificationTemplate");
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update template successfully")
        void shouldUpdateTemplateSuccessfully() {
            NotificationTemplateRequest request = buildTemplateRequest();
            NotificationTemplate template = buildTemplate();
            NotificationTemplateResponse response = buildTemplateResponse();

            when(templateRepository.findByIdAndTenantId(TEMPLATE_ID, TENANT_ID)).thenReturn(Optional.of(template));
            when(templateRepository.save(template)).thenReturn(template);
            when(templateMapper.toResponse(template)).thenReturn(response);

            NotificationTemplateResponse result = templateService.update(TEMPLATE_ID, request);

            assertThat(result).isNotNull();

            verify(templateMapper).updateEntity(request, template);
            verify(templateRepository).save(template);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when template not found for update")
        void shouldThrowWhenNotFoundForUpdate() {
            NotificationTemplateRequest request = buildTemplateRequest();

            when(templateRepository.findByIdAndTenantId(TEMPLATE_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> templateService.update(TEMPLATE_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("NotificationTemplate");

            verify(templateMapper, never()).updateEntity(any(), any());
            verify(templateRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should soft-delete template by setting deleted=true and active=false")
        void shouldSoftDeleteTemplate() {
            NotificationTemplate template = buildTemplate();

            when(templateRepository.findByIdAndTenantId(TEMPLATE_ID, TENANT_ID)).thenReturn(Optional.of(template));

            templateService.delete(TEMPLATE_ID);

            assertThat(template.isDeleted()).isTrue();
            assertThat(template.isActive()).isFalse();

            verify(templateRepository).save(template);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when template not found for delete")
        void shouldThrowWhenNotFoundForDelete() {
            when(templateRepository.findByIdAndTenantId(TEMPLATE_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> templateService.delete(TEMPLATE_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("NotificationTemplate");

            verify(templateRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getByEstateId")
    class GetByEstateId {

        @Test
        @DisplayName("should return templates for estate")
        void shouldReturnTemplatesForEstate() {
            NotificationTemplate template = buildTemplate();
            template.setEstateId(ESTATE_ID);
            NotificationTemplateResponse response = buildTemplateResponse();

            when(templateRepository.findByEstateIdAndTenantId(ESTATE_ID, TENANT_ID)).thenReturn(List.of(template));
            when(templateMapper.toResponse(template)).thenReturn(response);

            List<NotificationTemplateResponse> result = templateService.getByEstateId(ESTATE_ID);

            assertThat(result).hasSize(1);

            verify(templateRepository).findByEstateIdAndTenantId(ESTATE_ID, TENANT_ID);
        }
    }

    @Nested
    @DisplayName("resolveBody")
    class ResolveBody {

        @Test
        @DisplayName("should resolve body from global channel-specific template")
        void shouldResolveFromGlobalChannelSpecific() {
            NotificationTemplate template = buildTemplate();

            when(templateRepository.findByNotificationTypeAndChannelAndTenantId(
                    NotificationType.PAYMENT_RECEIVED, NotificationChannel.EMAIL, TENANT_ID))
                    .thenReturn(Optional.of(template));

            String result = templateService.resolveBody(
                    NotificationType.PAYMENT_RECEIVED, NotificationChannel.EMAIL, null,
                    Map.of("firstName", "Alice", "amount", "5000"));

            assertThat(result).isEqualTo("Hello Alice, your payment of 5000 was received.");
        }

        @Test
        @DisplayName("should resolve body from global channel-agnostic template when no channel-specific exists")
        void shouldFallBackToChannelAgnostic() {
            NotificationTemplate template = new NotificationTemplate();
            template.setBodyTemplate("Payment of {{amount}} received.");

            when(templateRepository.findByNotificationTypeAndChannelAndTenantId(
                    NotificationType.PAYMENT_RECEIVED, NotificationChannel.SMS, TENANT_ID))
                    .thenReturn(Optional.empty());
            when(templateRepository.findByNotificationTypeAndChannelIsNullAndTenantId(
                    NotificationType.PAYMENT_RECEIVED, TENANT_ID))
                    .thenReturn(Optional.of(template));

            String result = templateService.resolveBody(
                    NotificationType.PAYMENT_RECEIVED, NotificationChannel.SMS, null,
                    Map.of("amount", "3000"));

            assertThat(result).isEqualTo("Payment of 3000 received.");
        }

        @Test
        @DisplayName("should resolve body from estate-specific template first")
        void shouldPreferEstateSpecificTemplate() {
            NotificationTemplate estateTemplate = new NotificationTemplate();
            estateTemplate.setBodyTemplate("Estate: Payment of {{amount}} received.");

            when(templateRepository.findByNotificationTypeAndChannelAndEstateIdAndTenantId(
                    NotificationType.PAYMENT_RECEIVED, NotificationChannel.EMAIL, ESTATE_ID, TENANT_ID))
                    .thenReturn(Optional.of(estateTemplate));

            String result = templateService.resolveBody(
                    NotificationType.PAYMENT_RECEIVED, NotificationChannel.EMAIL, ESTATE_ID,
                    Map.of("amount", "7000"));

            assertThat(result).isEqualTo("Estate: Payment of 7000 received.");

            // Should not fall through to global lookups
            verify(templateRepository, never()).findByNotificationTypeAndChannelAndTenantId(any(), any(), any());
        }

        @Test
        @DisplayName("should return null when no template exists")
        void shouldReturnNullWhenNoTemplate() {
            when(templateRepository.findByNotificationTypeAndChannelAndTenantId(
                    NotificationType.GENERAL, NotificationChannel.SMS, TENANT_ID))
                    .thenReturn(Optional.empty());
            when(templateRepository.findByNotificationTypeAndChannelIsNullAndTenantId(
                    NotificationType.GENERAL, TENANT_ID))
                    .thenReturn(Optional.empty());

            String result = templateService.resolveBody(
                    NotificationType.GENERAL, NotificationChannel.SMS, null, null);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return raw template when data is null")
        void shouldReturnRawTemplateWhenNoData() {
            NotificationTemplate template = new NotificationTemplate();
            template.setBodyTemplate("Hello {{firstName}}, welcome!");

            when(templateRepository.findByNotificationTypeAndChannelAndTenantId(
                    NotificationType.GENERAL, NotificationChannel.EMAIL, TENANT_ID))
                    .thenReturn(Optional.of(template));

            String result = templateService.resolveBody(
                    NotificationType.GENERAL, NotificationChannel.EMAIL, null, null);

            assertThat(result).isEqualTo("Hello {{firstName}}, welcome!");
        }
    }

    @Nested
    @DisplayName("resolveSubject")
    class ResolveSubject {

        @Test
        @DisplayName("should resolve subject with template variables")
        void shouldResolveSubjectWithVariables() {
            NotificationTemplate template = buildTemplate();

            when(templateRepository.findByNotificationTypeAndChannelAndTenantId(
                    NotificationType.PAYMENT_RECEIVED, NotificationChannel.EMAIL, TENANT_ID))
                    .thenReturn(Optional.of(template));

            String result = templateService.resolveSubject(
                    NotificationType.PAYMENT_RECEIVED, NotificationChannel.EMAIL, null,
                    Map.of("amount", "5000"));

            assertThat(result).isEqualTo("Payment Confirmation for 5000");
        }

        @Test
        @DisplayName("should return null when template has no subject template")
        void shouldReturnNullWhenNoSubjectTemplate() {
            NotificationTemplate template = new NotificationTemplate();
            template.setBodyTemplate("Some body");
            template.setSubjectTemplate(null);

            when(templateRepository.findByNotificationTypeAndChannelAndTenantId(
                    NotificationType.GENERAL, NotificationChannel.EMAIL, TENANT_ID))
                    .thenReturn(Optional.of(template));

            String result = templateService.resolveSubject(
                    NotificationType.GENERAL, NotificationChannel.EMAIL, null, null);

            assertThat(result).isNull();
        }
    }
}
