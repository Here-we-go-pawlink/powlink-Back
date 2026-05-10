package hwan.project2.service.community;

import hwan.project2.domain.community.CommunityPost;
import hwan.project2.domain.community.ReactionType;
import hwan.project2.domain.community.repo.CommunityCommentRepository;
import hwan.project2.domain.community.repo.CommunityPostRepository;
import hwan.project2.domain.community.repo.CommunityReactionRepository;
import hwan.project2.domain.member.Member;
import hwan.project2.domain.member.repo.MemberRepository;
import hwan.project2.exception.auth.MemberNotFoundException;
import hwan.project2.exception.community.PostNotFoundException;
import hwan.project2.web.dto.community.PostCreateRequest;
import hwan.project2.web.dto.community.PostListItemResponse;
import hwan.project2.web.dto.community.PostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityPostService {

    private final CommunityPostRepository postRepository;
    private final MemberRepository memberRepository;
    private final CommunityReactionRepository reactionRepository;
    private final CommunityCommentRepository commentRepository;

    @Transactional
    public Long create(Long memberId, PostCreateRequest req) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        CommunityPost post = CommunityPost.create(member, req.emotionLabel(), req.title(), req.content(), req.tags());
        return postRepository.save(post).getId();
    }

    @Transactional(readOnly = true)
    public Page<PostListItemResponse> getList(Long memberId, String emotionLabel, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<CommunityPost> posts = (emotionLabel == null || emotionLabel.isBlank())
                ? postRepository.findAllByOrderByCreatedAtDesc(pageable)
                : postRepository.findByEmotionLabelOrderByCreatedAtDesc(emotionLabel, pageable);

        List<Long> ids = posts.stream().map(CommunityPost::getId).toList();
        Map<Long, Map<ReactionType, Long>> reactionMap = buildReactionMap(ids);
        Map<Long, Long> commentCountMap = buildCommentCountMap(ids);

        return posts.map(p -> PostListItemResponse.of(
                p,
                memberId,
                reactionMap.getOrDefault(p.getId(), Map.of()),
                commentCountMap.getOrDefault(p.getId(), 0L)
        ));
    }

    @Transactional(readOnly = true)
    public PostResponse getOne(Long memberId, Long postId) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        Map<Long, Map<ReactionType, Long>> reactionMap = buildReactionMap(List.of(postId));
        long commentCount = commentRepository.countByPostId(postId);

        return PostResponse.of(
                post,
                memberId,
                reactionMap.getOrDefault(postId, Map.of()),
                commentCount
        );
    }

    @Transactional
    public void update(Long memberId, Long postId, PostCreateRequest req) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
        if (!post.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인 게시글만 수정할 수 있습니다.");
        }
        post.update(req.emotionLabel(), req.title(), req.content(), req.tags());
    }

    @Transactional
    public void delete(Long memberId, Long postId) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
        if (!post.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인 게시글만 삭제할 수 있습니다.");
        }
        postRepository.delete(post);
    }

    private Map<Long, Map<ReactionType, Long>> buildReactionMap(List<Long> postIds) {
        if (postIds.isEmpty()) return Map.of();
        return reactionRepository.countActiveByPostIds(postIds)
                .stream()
                .collect(Collectors.groupingBy(
                        CommunityReactionRepository.ReactionCountView::getPostId,
                        Collectors.toMap(
                                CommunityReactionRepository.ReactionCountView::getReactionType,
                                CommunityReactionRepository.ReactionCountView::getCount
                        )
                ));
    }

    private Map<Long, Long> buildCommentCountMap(List<Long> postIds) {
        if (postIds.isEmpty()) return Map.of();
        return commentRepository.countByPostIds(postIds)
                .stream()
                .collect(Collectors.toMap(
                        CommunityCommentRepository.CommentCountView::getPostId,
                        CommunityCommentRepository.CommentCountView::getCount
                ));
    }
}
