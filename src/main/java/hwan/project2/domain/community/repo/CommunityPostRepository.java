package hwan.project2.domain.community.repo;

import hwan.project2.domain.community.CommunityPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    Page<CommunityPost> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<CommunityPost> findByEmotionLabelOrderByCreatedAtDesc(String emotionLabel, Pageable pageable);
}
