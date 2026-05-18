package hwan.project2.web.admin;

import hwan.project2.domain.notification.NotificationType;
import hwan.project2.security.UserPrincipal;
import hwan.project2.service.notification.DailyReminderScheduler;
import hwan.project2.service.notification.NotificationService;
import hwan.project2.service.report.WeeklyReportGenerationService;
import hwan.project2.service.report.WeeklyReportScheduler;
import hwan.project2.service.stats.MonthlyInsightScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 개발/테스트용 수동 트리거 컨트롤러.
 * prod 프로파일에서는 빈 자체가 등록되지 않음.
 * 인증된 사용자라면 누구나 호출 가능 (ADMIN 불필요).
 */
@RestController
@RequestMapping("/api/dev")
@Profile("!prod")
@RequiredArgsConstructor
public class DevController {

    private final NotificationService notificationService;
    private final WeeklyReportGenerationService weeklyReportGenerationService;
    private final WeeklyReportScheduler weeklyReportScheduler;
    private final MonthlyInsightScheduler monthlyInsightScheduler;
    private final DailyReminderScheduler dailyReminderScheduler;

    /**
     * 현재 로그인한 사용자에게 테스트 알림 즉시 발송 + SSE 푸시.
     * type: ANALYSIS | WEEKLY | LETTER | FRIEND | DAILY_REMINDER
     */
    @PostMapping("/notify")
    public ResponseEntity<String> sendTestNotification(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "ANALYSIS") String type,
            @RequestParam(defaultValue = "테스트 알림") String title,
            @RequestParam(defaultValue = "이것은 개발용 테스트 알림입니다") String body,
            @RequestParam(defaultValue = "/home") String link) {

        NotificationType nType = parseType(type);
        notificationService.create(principal.getId(), nType, title, body, link);
        return ResponseEntity.ok("알림 전송 완료 → memberId=" + principal.getId() + ", type=" + nType);
    }

    /**
     * 주간 리포트 생성.
     * startDate / endDate 를 넘기면 그 범위로, 안 넘기면 스케줄러 기본 로직(이번 주) 사용.
     * 예) POST /api/dev/weekly-report?startDate=2026-05-01&endDate=2026-05-07
     *    → 해당 기간 COMPLETED 일기가 3개 이상인 경우에만 생성됨
     */
    @PostMapping("/weekly-report")
    public ResponseEntity<String> triggerWeeklyReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusDays(6);
        LocalDate end   = endDate   != null ? LocalDate.parse(endDate)   : LocalDate.now();
        weeklyReportGenerationService.generateForDev(principal.getId(), start, end);
        return ResponseEntity.ok("주간 리포트 생성 완료: " + start + " ~ " + end);
    }

    /**
     * 월간 인사이트 생성.
     * yearMonth 미지정 시 전달 기준.
     * 예) POST /api/dev/monthly-insight?yearMonth=2026-04
     */
    @PostMapping("/monthly-insight")
    public ResponseEntity<String> triggerMonthlyInsight(
            @RequestParam(required = false) String yearMonth) {

        YearMonth ym = yearMonth != null
                ? YearMonth.parse(yearMonth)
                : YearMonth.now().minusMonths(1);
        monthlyInsightScheduler.generateMonthlyInsightsFor(ym);
        return ResponseEntity.ok("월간 인사이트 생성 완료: " + ym);
    }

    /**
     * 일기 작성 알림 수동 트리거.
     * notifyDailyReminder = true 인 ACTIVE 멤버 전체에게 발송.
     */
    @PostMapping("/daily-reminder")
    public ResponseEntity<String> triggerDailyReminder() {
        dailyReminderScheduler.sendDailyReminders();
        return ResponseEntity.ok("일기 작성 알림 발송 완료");
    }

    /**
     * 현재 로그인 사용자의 미배달 편지를 즉시 배달 처리 + 알림 발송.
     * 발표/테스트 시 편지가 하루 뒤 오는 것을 기다리지 않고 바로 확인 가능.
     */
    @PostMapping("/deliver-letters")
    public ResponseEntity<String> deliverPendingLetters(
            @AuthenticationPrincipal UserPrincipal principal) {
        int count = dailyReminderScheduler.deliverPendingLettersForMember(principal.getId());
        return ResponseEntity.ok("편지 " + count + "건 즉시 배달 완료");
    }

    private NotificationType parseType(String type) {
        try {
            return NotificationType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NotificationType.ANALYSIS;
        }
    }
}
