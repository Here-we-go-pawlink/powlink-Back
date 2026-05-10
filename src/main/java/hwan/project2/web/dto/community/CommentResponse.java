package hwan.project2.web.dto.community;

import hwan.project2.domain.community.CommunityComment;

import java.time.LocalDateTime;

public record CommentResponse(
        Long id,
        String authorName,
        String authorTag,
        String content,
        LocalDateTime createdAt,
        boolean isMine,
        boolean isHidden
) {
    public static CommentResponse of(CommunityComment comment, Long currentMemberId) {
        return new CommentResponse(
                comment.getId(),
                comment.getMember().getName(),
                comment.getMember().getTag(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getMember().getId().equals(currentMemberId),
                comment.isHidden()
        );
    }
}
