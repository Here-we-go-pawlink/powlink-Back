package hwan.project2.web.community;

import hwan.project2.security.UserPrincipal;
import hwan.project2.service.community.CommunityCommentService;
import hwan.project2.service.community.CommunityPostService;
import hwan.project2.service.community.CommunityReactionService;
import hwan.project2.web.dto.community.CommentCreateRequest;
import hwan.project2.web.dto.community.CommentResponse;
import hwan.project2.web.dto.community.PostCreateRequest;
import hwan.project2.web.dto.community.PostListItemResponse;
import hwan.project2.web.dto.community.PostResponse;
import hwan.project2.web.dto.community.ReactionToggleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/community/posts")
@RequiredArgsConstructor
public class CommunityPostController {

    private final CommunityPostService postService;
    private final CommunityCommentService commentService;
    private final CommunityReactionService reactionService;

    @GetMapping
    public Page<PostListItemResponse> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String emotionLabel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return postService.getList(principal.getId(), emotionLabel, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Long create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PostCreateRequest req
    ) {
        return postService.create(principal.getId(), req);
    }

    @GetMapping("/{postId}")
    public PostResponse getOne(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId
    ) {
        return postService.getOne(principal.getId(), postId);
    }

    @PutMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId,
            @Valid @RequestBody PostCreateRequest req
    ) {
        postService.update(principal.getId(), postId, req);
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId
    ) {
        postService.delete(principal.getId(), postId);
    }

    // ── 반응 ──────────────────────────────────────────────

    @PostMapping("/{postId}/reactions/{type}")
    public ReactionToggleResponse toggleReaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId,
            @PathVariable String type
    ) {
        return reactionService.toggle(principal.getId(), postId, type);
    }

    // ── 댓글 ──────────────────────────────────────────────

    @GetMapping("/{postId}/comments")
    public List<CommentResponse> getComments(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId
    ) {
        return commentService.getList(principal.getId(), postId);
    }

    @PostMapping("/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public Long addComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest req
    ) {
        return commentService.create(principal.getId(), postId, req);
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        commentService.delete(principal.getId(), commentId);
    }
}
