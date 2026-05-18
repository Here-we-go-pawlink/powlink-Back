package hwan.project2.domain.letter.repo;

import hwan.project2.domain.letter.Letter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LetterRepository extends JpaRepository<Letter, Long> {

    // 배달 가능한 편지 목록 (deliverAt <= now), 최신순 — diary LEFT JOIN FETCH로 N+1 방지
    @Query("SELECT l FROM Letter l LEFT JOIN FETCH l.diary WHERE l.member.id = :memberId AND l.deliverAt <= :now ORDER BY l.deliverAt DESC")
    List<Letter> findByMemberIdAndDeliverAtLessThanEqualOrderByDeliverAtDesc(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    // 읽지 않은 편지 수 (배달된 것만)
    long countByMemberIdAndIsReadFalseAndDeliverAtLessThanEqual(Long memberId, LocalDateTime now);

    Optional<Letter> findByIdAndMemberId(Long id, Long memberId);

    // 09:00 스케줄러용: member fetch join으로 N+1 방지
    @Query("SELECT l FROM Letter l JOIN FETCH l.member WHERE l.deliverAt BETWEEN :start AND :end")
    List<Letter> findByDeliverAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // dev용: 아직 배달되지 않은(미래 deliverAt) 편지 — member fetch join
    @Query("SELECT l FROM Letter l JOIN FETCH l.member WHERE l.member.id = :memberId AND l.deliverAt > :now")
    List<Letter> findByMemberIdAndDeliverAtAfter(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);
}
