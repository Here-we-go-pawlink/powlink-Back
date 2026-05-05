package hwan.project2.web.admin;

import hwan.project2.service.letter.LetterService;
import hwan.project2.service.report.WeeklyReportScheduler;
import hwan.project2.service.stats.MonthlyInsightScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final WeeklyReportScheduler weeklyReportScheduler;
    private final MonthlyInsightScheduler monthlyInsightScheduler;
    private final LetterService letterService;

    @GetMapping("/ping")
    public String ping() {
        return "admin pong";
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("admin ok");
    }

    /** 주간 리포트 스케줄러 수동 트리거 */
    @PostMapping("/weekly-report/trigger")
    public ResponseEntity<String> triggerWeeklyReport() {
        weeklyReportScheduler.generateWeeklyReports();
        return ResponseEntity.ok("주간 리포트 생성 완료");
    }

    /** 월간 인사이트 스케줄러 수동 트리거 */
    @PostMapping("/monthly-insight/trigger")
    public ResponseEntity<String> triggerMonthlyInsight(
            @RequestParam(required = false) String yearMonth) {
        java.time.YearMonth ym = yearMonth != null
                ? java.time.YearMonth.parse(yearMonth)
                : java.time.YearMonth.now().minusMonths(1);
        monthlyInsightScheduler.generateMonthlyInsightsFor(ym);
        return ResponseEntity.ok("월간 인사이트 생성 완료: " + ym);
    }

    /** 편지 즉시 생성 (테스트용) - AI 분석 완료된 일기 ID 필요 */
    @PostMapping("/letter/trigger")
    public ResponseEntity<String> triggerLetter(
            @RequestParam Long memberId,
            @RequestParam Long diaryId) {
        letterService.createImmediateLetter(diaryId, memberId);
        return ResponseEntity.ok("편지 생성 완료: diaryId=" + diaryId);
    }
}
