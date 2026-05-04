package hwan.project2.domain.diary.repo;

import hwan.project2.domain.diary.WeeklyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface WeeklyReportRepository extends JpaRepository<WeeklyReport, Long> {

    boolean existsByMemberIdAndStartDate(Long memberId, LocalDate startDate);
}
