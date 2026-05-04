package hwan.project2.domain.stats.repo;

import hwan.project2.domain.stats.MonthlyInsight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonthlyInsightRepository extends JpaRepository<MonthlyInsight, Long> {

    Optional<MonthlyInsight> findByMemberIdAndYearMonth(Long memberId, String yearMonth);

    boolean existsByMemberIdAndYearMonth(Long memberId, String yearMonth);
}
