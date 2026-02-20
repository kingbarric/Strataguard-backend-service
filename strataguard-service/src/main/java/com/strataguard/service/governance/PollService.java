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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PollService {

    private final PollRepository pollRepository;
    private final PollOptionRepository optionRepository;
    private final PollVoteRepository voteRepository;
    private final PollMapper pollMapper;

    public PollResponse createPoll(CreatePollRequest request, String userId, String userName) {
        UUID tenantId = TenantContext.requireTenantId();

        Poll poll = new Poll();
        poll.setTenantId(tenantId);
        poll.setEstateId(request.getEstateId());
        poll.setTitle(request.getTitle());
        poll.setDescription(request.getDescription());
        poll.setCreatedByUserId(userId);
        poll.setCreatedByName(userName);
        poll.setDeadline(request.getDeadline());
        poll.setStartsAt(request.getStartsAt());
        poll.setAllowMultipleChoices(request.isAllowMultipleChoices());
        poll.setAnonymous(request.isAnonymous());
        poll.setAllowProxyVoting(request.isAllowProxyVoting());
        poll.setEligibleVoterCount(request.getEligibleVoterCount());

        Poll saved = pollRepository.save(poll);

        // Create options
        List<PollOption> options = new ArrayList<>();
        for (int i = 0; i < request.getOptions().size(); i++) {
            PollOption option = new PollOption();
            option.setTenantId(tenantId);
            option.setPollId(saved.getId());
            option.setOptionText(request.getOptions().get(i));
            option.setDisplayOrder(i);
            options.add(option);
        }
        optionRepository.saveAll(options);

        log.info("Created poll: {} with {} options for tenant: {}", saved.getId(), options.size(), tenantId);
        return buildPollResponse(saved, options);
    }

    @Transactional(readOnly = true)
    public PollResponse getPoll(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();

        Poll poll = pollRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Poll", "id", id));

        List<PollOption> options = optionRepository.findByPollIdAndTenantId(id, tenantId);
        return buildPollResponse(poll, options);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PollResponse> getPollsByEstate(UUID estateId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Poll> page = pollRepository.findByEstateIdAndTenantId(estateId, tenantId, pageable);
        return toPagedResponse(page, tenantId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PollResponse> getActivePollsByEstate(UUID estateId, Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        Page<Poll> page = pollRepository.findByEstateIdAndStatusAndTenantId(estateId, PollStatus.ACTIVE, tenantId, pageable);
        return toPagedResponse(page, tenantId);
    }

    public PollResponse activatePoll(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();

        Poll poll = pollRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Poll", "id", id));

        if (poll.getStatus() != PollStatus.DRAFT) {
            throw new IllegalStateException("Only draft polls can be activated");
        }

        poll.setStatus(PollStatus.ACTIVE);
        if (poll.getStartsAt() == null) {
            poll.setStartsAt(Instant.now());
        }
        Poll saved = pollRepository.save(poll);

        List<PollOption> options = optionRepository.findByPollIdAndTenantId(id, tenantId);
        log.info("Activated poll: {} for tenant: {}", id, tenantId);
        return buildPollResponse(saved, options);
    }

    public PollResponse closePoll(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();

        Poll poll = pollRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Poll", "id", id));

        if (poll.getStatus() != PollStatus.ACTIVE) {
            throw new IllegalStateException("Only active polls can be closed");
        }

        poll.setStatus(PollStatus.CLOSED);
        Poll saved = pollRepository.save(poll);

        List<PollOption> options = optionRepository.findByPollIdAndTenantId(id, tenantId);
        log.info("Closed poll: {} for tenant: {}", id, tenantId);
        return buildPollResponse(saved, options);
    }

    public void castVote(UUID pollId, UUID voterId, CastVoteRequest request) {
        UUID tenantId = TenantContext.requireTenantId();

        Poll poll = pollRepository.findByIdAndTenantId(pollId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Poll", "id", pollId));

        if (poll.getStatus() != PollStatus.ACTIVE) {
            throw new IllegalStateException("Poll is not active");
        }

        if (Instant.now().isAfter(poll.getDeadline())) {
            throw new IllegalStateException("Poll deadline has passed");
        }

        // Check if voter already voted
        boolean alreadyVoted = voteRepository.existsByPollIdAndVoterId(pollId, voterId, tenantId);
        if (alreadyVoted) {
            throw new IllegalStateException("You have already voted on this poll");
        }

        if (!poll.isAllowMultipleChoices() && request.getOptionIds().size() > 1) {
            throw new IllegalArgumentException("This poll allows only one choice");
        }

        boolean isProxy = request.getProxyForId() != null;
        if (isProxy && !poll.isAllowProxyVoting()) {
            throw new IllegalArgumentException("This poll does not allow proxy voting");
        }

        List<PollOption> options = optionRepository.findByPollIdAndTenantId(pollId, tenantId);
        List<UUID> validOptionIds = options.stream().map(PollOption::getId).toList();

        for (UUID optionId : request.getOptionIds()) {
            if (!validOptionIds.contains(optionId)) {
                throw new IllegalArgumentException("Invalid option: " + optionId);
            }

            PollVote vote = new PollVote();
            vote.setTenantId(tenantId);
            vote.setPollId(pollId);
            vote.setOptionId(optionId);
            vote.setVoterId(voterId);
            vote.setProxyForId(request.getProxyForId());
            vote.setProxyVote(isProxy);
            voteRepository.save(vote);

            // Increment vote count on option
            PollOption option = options.stream()
                    .filter(o -> o.getId().equals(optionId))
                    .findFirst()
                    .orElseThrow();
            option.setVoteCount(option.getVoteCount() + 1);
            optionRepository.save(option);
        }

        // Increment total votes on poll
        poll.setTotalVotes(poll.getTotalVotes() + 1);
        pollRepository.save(poll);

        log.info("Vote cast on poll: {} by voter: {} for tenant: {}", pollId, voterId, tenantId);
    }

    public void deletePoll(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();

        Poll poll = pollRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Poll", "id", id));

        poll.setDeleted(true);
        pollRepository.save(poll);
        log.info("Soft-deleted poll: {} for tenant: {}", id, tenantId);
    }

    private PollResponse buildPollResponse(Poll poll, List<PollOption> options) {
        PollResponse response = pollMapper.toResponse(poll);
        List<PollOptionResponse> optionResponses = options.stream()
                .map(o -> {
                    PollOptionResponse optionResponse = pollMapper.toOptionResponse(o);
                    if (poll.getTotalVotes() > 0) {
                        optionResponse.setPercentage((double) o.getVoteCount() / poll.getTotalVotes() * 100);
                    }
                    return optionResponse;
                })
                .toList();
        response.setOptions(optionResponses);
        return response;
    }

    private PagedResponse<PollResponse> toPagedResponse(Page<Poll> page, UUID tenantId) {
        List<PollResponse> content = page.getContent().stream()
                .map(poll -> {
                    List<PollOption> options = optionRepository.findByPollIdAndTenantId(poll.getId(), tenantId);
                    return buildPollResponse(poll, options);
                })
                .toList();
        return PagedResponse.<PollResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
