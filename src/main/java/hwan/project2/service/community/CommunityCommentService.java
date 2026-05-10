package hwan.project2.service.community;

import hwan.project2.domain.community.CommunityComment;
import hwan.project2.domain.community.CommunityPost;
import hwan.project2.domain.community.repo.CommunityCommentRepository;
import hwan.project2.domain.community.repo.CommunityPostRepository;
import hwan.project2.domain.member.Member;
import hwan.project2.domain.member.repo.MemberRepository;
import hwan.project2.exception.auth.MemberNotFoundException;
import hwan.project2.exception.community.PostNotFoundException;
import hwan.project2.web.dto.community.CommentCreateRequest;
import hwan.project2.web.dto.community.CommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityCommentService {

    private final CommunityCommentRepository commentRepository;
    private final CommunityPostRepository postRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Long create(Long memberId, Long postId, CommentCreateRequest req) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
        boolean hidden = Boolean.TRUE.equals(req.isHidden());
        CommunityComment comment = CommunityComment.create(post, member, req.content(), hidden);
        return commentRepository.save(comment).getId();
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getList(Long memberId, Long postId) {
        if (!postRepository.existsById(postId)) throw new PostNotFoundException();
        return commentRepository.findByPost_IdOrderByCreatedAtAsc(postId)
                .stream()
                .map(c -> CommentResponse.of(c, memberId))
                .toList();
    }

    @Transactional
    public void delete(Long memberId, Long commentId) {
        CommunityComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        if (!comment.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인 댓글만 삭제할 수 있습니다.");
        }
        commentRepository.delete(comment);
    }
}
