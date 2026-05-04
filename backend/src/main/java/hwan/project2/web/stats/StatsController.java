package hwan.project2.web.stats;

import hwan.project2.security.UserPrincipal;
import hwan.project2.service.stats.StatsService;
import hwan.project2.web.dto.stats.StatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /**
     * GET /api/stats?month=2026-05
     * month 생략 시 현재 달 기준
     */
    @GetMapping
    public StatsResponse getStats(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String month
    ) {
        YearMonth yearMonth = (month != null) ? YearMonth.parse(month) : YearMonth.now();
        return statsService.getStats(principal.getId(), yearMonth);
    }
}
