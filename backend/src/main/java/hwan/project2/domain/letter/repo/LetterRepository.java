package hwan.project2.domain.letter.repo;

import hwan.project2.domain.letter.Letter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LetterRepository extends JpaRepository<Letter, Long> {

    // 배달 가능한 편지 목록 (deliverAt <= now), 최신순
    List<Letter> findByMemberIdAndDeliverAtLessThanEqualOrderByDeliverAtDesc(Long memberId, LocalDateTime now);

    // 읽지 않은 편지 수 (배달된 것만)
    long countByMemberIdAndIsReadFalseAndDeliverAtLessThanEqual(Long memberId, LocalDateTime now);

    Optional<Letter> findByIdAndMemberId(Long id, Long memberId);
}
