package com.strataguard.service.governance;

import com.strataguard.core.config.TenantContext;
import com.strataguard.core.dto.common.PagedResponse;
import com.strataguard.core.dto.governance.*;
import com.strataguard.core.entity.Poll;
import com.strataguard.core.entity.PollOption;
import com.strataguard.core.entity.PollVote;
import com.strataguard.core.enums.PollStatus;
import com.strataguard.core.exception.ResourceNotFoundException;
import com.strataguard.core.util.PollMapper;
import com.strataguard.infrastructure.repository.PollOptionRepository;
import com.strataguard.infrastructure.repository.PollRepository;
import com.strataguard.infrastructure.repository.PollVoteRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID POLL_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ESTATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID VOTER_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID OPTION1_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID OPTION2_ID = UUID.fromString("00000000-0000-0000-0000-000000000006");

    @Mock private PollRepository pollRepository;
    @Mock private PollOptionRepository optionRepository;
    @Mock private PollVoteRepository voteRepository;
    @Mock private PollMapper pollMapper;

    @InjectMocks
    private PollService pollService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createPoll_success() {
        CreatePollRequest request = CreatePollRequest.builder()
                .estateId(ESTATE_ID)
                .title("Best color for gate?")
                .deadline(Instant.now().plus(7, ChronoUnit.DAYS))
                .options(List.of("Blue", "Green", "White"))
                .build();

        Poll poll = new Poll();
        poll.setId(POLL_ID);
        poll.setTotalVotes(0);
        PollResponse expectedResponse = PollResponse.builder().id(POLL_ID).title("Best color for gate?").build();
        PollOptionResponse optionResponse = PollOptionResponse.builder().optionText("Blue").build();

        when(pollRepository.save(any(Poll.class))).thenReturn(poll);
        when(optionRepository.saveAll(anyList())).thenReturn(List.of());
        when(pollMapper.toResponse(poll)).thenReturn(expectedResponse);
        when(pollMapper.toOptionResponse(any(PollOption.class))).thenReturn(optionResponse);

        PollResponse result = pollService.createPoll(request, "user1", "Admin");

        assertThat(result.getId()).isEqualTo(POLL_ID);
        verify(pollRepository).save(any(Poll.class));
        verify(optionRepository).saveAll(anyList());
    }

    @Test
    void getPoll_success() {
        Poll poll = new Poll();
        poll.setId(POLL_ID);
        poll.setTotalVotes(0);
        PollResponse expectedResponse = PollResponse.builder().id(POLL_ID).build();

        when(pollRepository.findByIdAndTenantId(POLL_ID, TENANT_ID)).thenReturn(Optional.of(poll));
        when(optionRepository.findByPollIdAndTenantId(POLL_ID, TENANT_ID)).thenReturn(List.of());
        when(pollMapper.toResponse(poll)).thenReturn(expectedResponse);

        PollResponse result = pollService.getPoll(POLL_ID);

        assertThat(result.getId()).isEqualTo(POLL_ID);
    }

    @Test
    void getPoll_notFound() {
        when(pollRepository.findByIdAndTenantId(POLL_ID, TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pollService.getPoll(POLL_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void activatePoll_success() {
        Poll poll = new Poll();
        poll.setId(POLL_ID);
        poll.setStatus(PollStatus.DRAFT);
        poll.setTotalVotes(0);
        PollResponse expectedResponse = PollResponse.builder().id(POLL_ID).status(PollStatus.ACTIVE).build();

        when(pollRepository.findByIdAndTenantId(POLL_ID, TENANT_ID)).thenReturn(Optional.of(poll));
        when(pollRepository.save(poll)).thenReturn(poll);
        when(optionRepository.findByPollIdAndTenantId(POLL_ID, TENANT_ID)).thenReturn(List.of());
        when(pollMapper.toResponse(poll)).thenReturn(expectedResponse);

        PollResponse result = pollService.activatePoll(POLL_ID);

        assertThat(poll.getStatus()).isEqualTo(PollStatus.ACTIVE);
        assertThat(poll.getStartsAt()).isNotNull();
    }

    @Test
    void activatePoll_notDraft() {
        Poll poll = new Poll();
        poll.setStatus(PollStatus.ACTIVE);

        when(pollRepository.findByIdAndTenantId(POLL_ID, TENANT_ID)).thenReturn(Optional.of(poll));

        assertThatThrownBy(() -> pollService.activatePoll(POLL_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("draft");
    }

    @Test
    void closePoll_success() {
        Poll poll = new Poll();
        poll.setId(POLL_ID);
        poll.setStatus(PollStatus.ACTIVE);
        poll.setTotalVotes(0);
        PollResponse expectedResponse = PollResponse.builder().id(POLL_ID).status(PollStatus.CLOSED).build();

        when(pollRepository.findByIdAndTenantId(POLL_ID, TENANT_ID)).thenReturn(Optional.of(poll));
        when(pollRepository.save(poll)).thenReturn(poll);
        when(optionRepository.findByPollIdAndTenantId(POLL_ID, TENANT_ID)).thenReturn(List.of());
        when(pollMapper.toResponse(poll)).thenReturn(expectedResponse);

        pollService.closePoll(POLL_ID);

        assertThat(poll.getStatus()).isEqualTo(PollStatus.CLOSED);
    }

    @Test
    void closePoll_notActive() {
        Poll poll = new Poll();
        poll.setStatus(PollStatus.DRAFT);

        when(pollRepository.findByIdAndTenantId(POLL_ID, TENANT_ID)).thenReturn(Optional.of(poll));

        assertThatThrownBy(() -> pollService.closePoll(POLL_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active");
    }

    @Test
    void castVote_success() {
        Poll poll = new Poll();
        poll.setId(POLL_ID);
        poll.setStatus(PollStatus.ACTIVE);
        poll.setDeadline(Instant.now().plus(1, ChronoUnit.DAYS));
        poll.setAllowMultipleChoices(false);
        poll.setAllowProxyVoting(false);
        poll.setTotalVotes(0);

        PollOption option1 = new PollOption();
        option1.setId(OPTION1_ID);
        option1.setVoteCount(0);

        CastVoteRequest request = CastVoteRequest.builder().optionIds(List.of(OPTION1_ID)).build();

        when(pollRepository.findByIdAndTenantId(POLL_ID, TENANT_ID)).thenReturn(Optional.of(poll));
        when(voteRepository.existsByPollIdAndVoterId(POLL_ID, VOTER_ID, TENANT_ID)).thenReturn(false);
        when(optionRepository.findByPollIdAndTenantId(POLL_ID, TENANT_ID)).thenReturn(List.of(option1));
        when(voteRepository.save(any(PollVote.class))).thenReturn(new PollVote());

        pollService.castVote(POLL_ID, VOTER_ID, request);

        assertThat(option1.getVoteCount()).isEqualTo(1);
        assertThat(poll.getTotalVotes()).isEqualTo(1);
        verify(voteRepository).save(any(PollVote.class));
    }

    @Test
    void castVote_alreadyVoted() {
        Poll poll = new Poll();
        poll.setStatus(PollStatus.ACTIVE);
        poll.setDeadline(Instant.now().plus(1, ChronoUnit.DAYS));

        when(pollRepository.findByIdAndTenantId(POLL_ID, TENANT_ID)).thenReturn(Optional.of(poll));
        when(voteRepository.existsByPollIdAndVoterId(POLL_ID, VOTER_ID, TENANT_ID)).thenReturn(true);

        CastVoteRequest request = CastVoteRequest.builder().optionIds(List.of(OPTION1_ID)).build();

        assertThatThrownBy(() -> pollService.castVote(POLL_ID, VOTER_ID, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already voted");
    }

    @Test
    void castVote_pollNotActive() {
        Poll poll = new Poll();
        poll.setStatus(PollStatus.CLOSED);

        when(pollRepository.findByIdAndTenantId(POLL_ID, TENANT_ID)).thenReturn(Optional.of(poll));

        CastVoteRequest request = CastVoteRequest.builder().optionIds(List.of(OPTION1_ID)).build();

        assertThatThrownBy(() -> pollService.castVote(POLL_ID, VOTER_ID, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void castVote_deadlinePassed() {
        Poll poll = new Poll();
        poll.setStatus(PollStatus.ACTIVE);
        poll.setDeadline(Instant.now().minus(1, ChronoUnit.DAYS));

        when(pollRepository.findByIdAndTenantId(POLL_ID, TENANT_ID)).thenReturn(Optional.of(poll));

        CastVoteRequest request = CastVoteRequest.builder().optionIds(List.of(OPTION1_ID)).build();

        assertThatThrownBy(() -> pollService.castVote(POLL_ID, VOTER_ID, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deadline");
    }

    @Test
    void castVote_multipleChoicesNotAllowed() {
        Poll poll = new Poll();
        poll.setStatus(PollStatus.ACTIVE);
        poll.setDeadline(Instant.now().plus(1, ChronoUnit.DAYS));
        poll.setAllowMultipleChoices(false);

        when(pollRepository.findByIdAndTenantId(POLL_ID, TENANT_ID)).thenReturn(Optional.of(poll));
        when(voteRepository.existsByPollIdAndVoterId(POLL_ID, VOTER_ID, TENANT_ID)).thenReturn(false);

        CastVoteRequest request = CastVoteRequest.builder().optionIds(List.of(OPTION1_ID, OPTION2_ID)).build();

        assertThatThrownBy(() -> pollService.castVote(POLL_ID, VOTER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("one choice");
    }

    @Test
    void deletePoll_success() {
        Poll poll = new Poll();
        poll.setId(POLL_ID);

        when(pollRepository.findByIdAndTenantId(POLL_ID, TENANT_ID)).thenReturn(Optional.of(poll));

        pollService.deletePoll(POLL_ID);

        assertThat(poll.isDeleted()).isTrue();
        verify(pollRepository).save(poll);
    }
}
