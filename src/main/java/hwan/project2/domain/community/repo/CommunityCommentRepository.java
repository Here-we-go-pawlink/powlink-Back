package hwan.project2.domain.community.repo;

import hwan.project2.domain.community.CommunityComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {

    interface CommentCountView {
        Long getPostId();
        Long getCount();
    }

    List<CommunityComment> findByPost_IdOrderByCreatedAtAsc(Long postId);

    @Query("SELECT COUNT(c) FROM CommunityComment c WHERE c.post.id = :postId")
    long countByPostId(@Param("postId") Long postId);

    @Query("""
            SELECT c.post.id AS postId, COUNT(c) AS count
            FROM CommunityComment c
            WHERE c.post.id IN :postIds
            GROUP BY c.post.id
            """)
    List<CommentCountView> countByPostIds(@Param("postIds") List<Long> postIds);
}
