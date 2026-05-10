package hwan.project2.service.community;

import hwan.project2.domain.community.CommunityPost;
import hwan.project2.domain.community.CommunityReaction;
import hwan.project2.domain.community.ReactionType;
import hwan.project2.domain.community.repo.CommunityPostRepository;
import hwan.project2.domain.community.repo.CommunityReactionRepository;
import hwan.project2.domain.member.Member;
import hwan.project2.domain.member.repo.MemberRepository;
import hwan.project2.exception.auth.MemberNotFoundException;
import hwan.project2.exception.community.PostNotFoundException;
import hwan.project2.web.dto.community.ReactionToggleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommunityReactionService {

    private final CommunityReactionRepository reactionRepository;
    private final CommunityPostRepository postRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public ReactionToggleResponse toggle(Long memberId, Long postId, String typeStr) {
        ReactionType type = ReactionType.from(typeStr);

        Optional<CommunityReaction> existing =
                reactionRepository.findByPost_IdAndMember_IdAndReactionType(postId, memberId, type);

        boolean nowReacted;
        if (existing.isPresent()) {
            CommunityReaction reaction = existing.get();
            if (reaction.isActive()) {
                reaction.softDelete();
                nowReacted = false;
            } else {
                reaction.restore();
                nowReacted = true;
            }
        } else {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(MemberNotFoundException::new);
            CommunityPost post = postRepository.findById(postId)
                    .orElseThrow(PostNotFoundException::new);
            reactionRepository.save(CommunityReaction.create(post, member, type));
            nowReacted = true;
        }

        long count = reactionRepository.countActiveByPostIdAndType(postId, type);
        return new ReactionToggleResponse(nowReacted, count);
    }
}
