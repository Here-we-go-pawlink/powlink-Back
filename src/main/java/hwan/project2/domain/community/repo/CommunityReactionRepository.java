package hwan.project2.domain.community.repo;

import hwan.project2.domain.community.CommunityReaction;
import hwan.project2.domain.community.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommunityReactionRepository extends JpaRepository<CommunityReaction, Long> {

    interface ReactionCountView {
        Long getPostId();
        ReactionType getReactionType();
        Long getCount();
    }

    Optional<CommunityReaction> findByPost_IdAndMember_IdAndReactionType(
            Long postId, Long memberId, ReactionType reactionType);

    @Query("""
            SELECT r.post.id AS postId, r.reactionType AS reactionType, COUNT(r) AS count
            FROM CommunityReaction r
            WHERE r.post.id IN :postIds AND r.deletedAt IS NULL
            GROUP BY r.post.id, r.reactionType
            """)
    List<ReactionCountView> countActiveByPostIds(@Param("postIds") List<Long> postIds);

    @Query("SELECT COUNT(r) FROM CommunityReaction r WHERE r.post.id = :postId AND r.reactionType = :type AND r.deletedAt IS NULL")
    long countActiveByPostIdAndType(@Param("postId") Long postId, @Param("type") ReactionType type);
}
