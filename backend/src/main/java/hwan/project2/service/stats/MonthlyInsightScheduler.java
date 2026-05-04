package hwan.project2.service.stats;

import hwan.project2.domain.diary.repo.DiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyInsightScheduler {

    private final DiaryRepository diaryRepository;
    private final MonthlyInsightGenerationService monthlyInsightGenerationService;

    // 매월 1일 오전 9시 실행 (전달 분석)
    @Scheduled(cron = "0 0 9 1 * *")
    public void generateMonthlyInsights() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        LocalDate startDate = lastMonth.atDay(1);
        LocalDate endDate = lastMonth.atEndOfMonth();

        log.info("월간 인사이트 생성 시작: {}", lastMonth);

        List<Long> memberIds = diaryRepository.findMemberIdsWithEnoughDiaries(startDate, endDate, 3);
        log.info("대상 멤버 수: {}", memberIds.size());

        for (Long memberId : memberIds) {
            monthlyInsightGenerationService.generate(memberId, lastMonth);
        }

        log.info("월간 인사이트 생성 완료");
    }
}
