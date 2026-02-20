package com.strataguard.service.governance;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.governance.*;
import com.strataguard.core.entity.Announcement;
import com.strataguard.core.enums.AnnouncementAudience;
import com.strataguard.core.enums.AnnouncementPriority;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.AnnouncementMapper;
import com.strataguard.infrastructure.repository.AnnouncementRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ANNOUNCEMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ESTATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Mock private AnnouncementRepository announcementRepository;
    @Mock private AnnouncementMapper announcementMapper;

    @InjectMocks
    private AnnouncementService announcementService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createAnnouncement_success() {
        CreateAnnouncementRequest request = CreateAnnouncementRequest.builder()
                .estateId(ESTATE_ID)
                .title("Test Announcement")
                .body("Test body")
                .audience(AnnouncementAudience.ALL)
                .publishImmediately(false)
                .build();

        Announcement announcement = new Announcement();
        announcement.setId(ANNOUNCEMENT_ID);
        AnnouncementResponse expectedResponse = AnnouncementResponse.builder().id(ANNOUNCEMENT_ID).title("Test Announcement").build();

        when(announcementMapper.toEntity(request)).thenReturn(announcement);
        when(announcementRepository.save(any(Announcement.class))).thenReturn(announcement);
        when(announcementMapper.toResponse(announcement)).thenReturn(expectedResponse);

        AnnouncementResponse result = announcementService.createAnnouncement(request, "user1", "Admin User");

        assertThat(result.getId()).isEqualTo(ANNOUNCEMENT_ID);
        assertThat(announcement.getPostedBy()).isEqualTo("user1");
        assertThat(announcement.isPublished()).isFalse();
    }

    @Test
    void createAnnouncement_publishImmediately() {
        CreateAnnouncementRequest request = CreateAnnouncementRequest.builder()
                .estateId(ESTATE_ID)
                .title("Urgent")
                .body("Body")
                .audience(AnnouncementAudience.ALL)
                .publishImmediately(true)
                .build();

        Announcement announcement = new Announcement();
        announcement.setId(ANNOUNCEMENT_ID);
        AnnouncementResponse expectedResponse = AnnouncementResponse.builder().id(ANNOUNCEMENT_ID).published(true).build();

        when(announcementMapper.toEntity(request)).thenReturn(announcement);
        when(announcementRepository.save(any(Announcement.class))).thenReturn(announcement);
        when(announcementMapper.toResponse(announcement)).thenReturn(expectedResponse);

        AnnouncementResponse result = announcementService.createAnnouncement(request, "user1", "Admin");

        assertThat(announcement.isPublished()).isTrue();
        assertThat(announcement.getPublishedAt()).isNotNull();
    }

    @Test
    void getAnnouncement_success() {
        Announcement announcement = new Announcement();
        announcement.setId(ANNOUNCEMENT_ID);
        AnnouncementResponse expectedResponse = AnnouncementResponse.builder().id(ANNOUNCEMENT_ID).build();

        when(announcementRepository.findByIdAndTenantId(ANNOUNCEMENT_ID, TENANT_ID)).thenReturn(Optional.of(announcement));
        when(announcementMapper.toResponse(announcement)).thenReturn(expectedResponse);

        AnnouncementResponse result = announcementService.getAnnouncement(ANNOUNCEMENT_ID);

        assertThat(result.getId()).isEqualTo(ANNOUNCEMENT_ID);
    }

    @Test
    void getAnnouncement_notFound() {
        when(announcementRepository.findByIdAndTenantId(ANNOUNCEMENT_ID, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> announcementService.getAnnouncement(ANNOUNCEMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void publishAnnouncement_success() {
        Announcement announcement = new Announcement();
        announcement.setId(ANNOUNCEMENT_ID);
        announcement.setPublished(false);
        AnnouncementResponse expectedResponse = AnnouncementResponse.builder().id(ANNOUNCEMENT_ID).published(true).build();

        when(announcementRepository.findByIdAndTenantId(ANNOUNCEMENT_ID, TENANT_ID)).thenReturn(Optional.of(announcement));
        when(announcementRepository.save(announcement)).thenReturn(announcement);
        when(announcementMapper.toResponse(announcement)).thenReturn(expectedResponse);

        AnnouncementResponse result = announcementService.publishAnnouncement(ANNOUNCEMENT_ID);

        assertThat(announcement.isPublished()).isTrue();
        assertThat(announcement.getPublishedAt()).isNotNull();
    }

    @Test
    void publishAnnouncement_alreadyPublished() {
        Announcement announcement = new Announcement();
        announcement.setPublished(true);

        when(announcementRepository.findByIdAndTenantId(ANNOUNCEMENT_ID, TENANT_ID)).thenReturn(Optional.of(announcement));

        assertThatThrownBy(() -> announcementService.publishAnnouncement(ANNOUNCEMENT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already published");
    }

    @Test
    void unpublishAnnouncement_success() {
        Announcement announcement = new Announcement();
        announcement.setPublished(true);
        AnnouncementResponse expectedResponse = AnnouncementResponse.builder().id(ANNOUNCEMENT_ID).published(false).build();

        when(announcementRepository.findByIdAndTenantId(ANNOUNCEMENT_ID, TENANT_ID)).thenReturn(Optional.of(announcement));
        when(announcementRepository.save(announcement)).thenReturn(announcement);
        when(announcementMapper.toResponse(announcement)).thenReturn(expectedResponse);

        announcementService.unpublishAnnouncement(ANNOUNCEMENT_ID);

        assertThat(announcement.isPublished()).isFalse();
    }

    @Test
    void getAllAnnouncements_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Announcement> page = new PageImpl<>(List.of(), pageable, 0);

        when(announcementRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);

        PagedResponse<AnnouncementResponse> result = announcementService.getAllAnnouncements(pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void getActiveAnnouncements_success() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Announcement> page = new PageImpl<>(List.of(), pageable, 0);

        when(announcementRepository.findActiveByEstateId(eq(ESTATE_ID), eq(TENANT_ID), any(Instant.class), eq(pageable))).thenReturn(page);

        PagedResponse<AnnouncementResponse> result = announcementService.getActiveAnnouncements(ESTATE_ID, pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void deleteAnnouncement_success() {
        Announcement announcement = new Announcement();
        announcement.setId(ANNOUNCEMENT_ID);

        when(announcementRepository.findByIdAndTenantId(ANNOUNCEMENT_ID, TENANT_ID)).thenReturn(Optional.of(announcement));

        announcementService.deleteAnnouncement(ANNOUNCEMENT_ID);

        assertThat(announcement.isDeleted()).isTrue();
        verify(announcementRepository).save(announcement);
    }
}
