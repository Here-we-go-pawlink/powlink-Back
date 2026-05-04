package hwan.project2.service.report;

import hwan.project2.domain.diary.repo.DiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyReportScheduler {

    private static final int MIN_DIARY_COUNT = 3;

    private final DiaryRepository diaryRepository;
    private final WeeklyReportGenerationService weeklyReportGenerationService;

    // 매주 일요일 자정 실행
    @Scheduled(cron = "0 0 0 * * SUN")
    public void generateWeeklyReports() {
        LocalDate today = LocalDate.now(); // 일요일
        LocalDate startDate = today.with(DayOfWeek.MONDAY);
        LocalDate endDate = today;

        log.info("주간 리포트 생성 시작: {} ~ {}", startDate, endDate);

        List<Long> memberIds = diaryRepository.findMemberIdsWithEnoughDiaries(startDate, endDate, MIN_DIARY_COUNT);
        log.info("주간 리포트 대상 멤버 수: {}", memberIds.size());

        for (Long memberId : memberIds) {
            weeklyReportGenerationService.generate(memberId, startDate, endDate);
        }

        log.info("주간 리포트 생성 완료");
    }
}
