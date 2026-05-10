package hwan.project2.web.dto.community;

import hwan.project2.domain.community.CommunityPost;
import hwan.project2.domain.community.ReactionType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record PostResponse(
        Long id,
        String emotionLabel,
        String title,
        String content,
        List<String> tags,
        String authorName,
        String authorTag,
        LocalDateTime createdAt,
        boolean isMine,
        long commentCount,
        Map<String, Long> reactions,
        double similarity
) {
    public static PostResponse of(
            CommunityPost post,
            Long currentMemberId,
            Map<ReactionType, Long> reactions,
            long commentCount
    ) {
        return new PostResponse(
                post.getId(),
                post.getEmotionLabel(),
                post.getTitle(),
                post.getContent(),
                post.getTags(),
                post.getMember().getName(),
                post.getMember().getTag(),
                post.getCreatedAt(),
                post.getMember().getId().equals(currentMemberId),
                commentCount,
                Map.of(
                        "empathy",    reactions.getOrDefault(ReactionType.EMPATHY, 0L),
                        "comfort",    reactions.getOrDefault(ReactionType.COMFORT, 0L),
                        "understand", reactions.getOrDefault(ReactionType.UNDERSTAND, 0L)
                ),
                0.0
        );
    }
}
