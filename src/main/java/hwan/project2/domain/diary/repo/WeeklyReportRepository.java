package hwan.project2.domain.diary.repo;

import hwan.project2.domain.diary.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {

    boolean existsByMemberIdAndStartDate(Long memberId, LocalDate startDate);

    List<WeeklyReport> findByMemberIdOrderByStartDateDesc(Long memberId);

    Optional<WeeklyReport> findByIdAndMemberId(Long id, Long memberId);

    void deleteByMemberIdAndStartDate(Long memberId, LocalDate startDate);
}
