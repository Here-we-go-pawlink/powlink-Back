package hwan.project2.service.stats;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import hwan.project2.domain.stats.MonthlyInsight;
import hwan.project2.domain.stats.repo.MonthlyInsightRepository;
import hwan.project2.web.dto.stats.StatsResponse;
import hwan.project2.web.dto.stats.StatsResponse.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StatsService {

    private static final String CACHE_PREFIX = "stats:";
    private static final Duration CURRENT_MONTH_TTL = Duration.ofHours(1);
    private static final Duration PAST_MONTH_TTL = Duration.ofHours(24);

    private final StatsQueryService statsQueryService;
    private final MonthlyInsightRepository monthlyInsightRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StatsResponse getStats(Long memberId, YearMonth yearMonth) {
        String cacheKey = CACHE_PREFIX + memberId + ":" + yearMonth;

        // 1. 캐시 조회
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, StatsResponse.class);
            } catch (Exception e) {
                log.warn("통계 캐시 역직렬화 실패, 재계산: key={}", cacheKey);
            }
        }

        // 2. 계산
        StatsResponse stats = compute(memberId, yearMonth);

        // 3. 캐시 저장
        try {
            Duration ttl = yearMonth.equals(YearMonth.now()) ? CURRENT_MONTH_TTL : PAST_MONTH_TTL;
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(stats), ttl);
        } catch (Exception e) {
            log.warn("통계 캐시 저장 실패: key={}", cacheKey);
        }

        return stats;
    }

    public void evictCache(Long memberId) {
        String cacheKey = CACHE_PREFIX + memberId + ":" + YearMonth.now();
        redisTemplate.delete(cacheKey);
    }

    // ── 실제 계산 ──────────────────────────────────────────────────────────────

    private StatsResponse compute(Long memberId, YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<TrendPoint> trend = statsQueryService.emotionTrend(memberId, startDate, endDate);
        List<EmotionShare> dist = statsQueryService.emotionDistribution(memberId, startDate, endDate);

        SummaryStats summary = buildSummary(memberId, startDate, endDate, trend, dist);

        return new StatsResponse(
                summary,
                trend,
                dist,
                statsQueryService.keywords(memberId, startDate, endDate),
                statsQueryService.dowPattern(memberId, startDate, endDate),
                statsQueryService.timeSlotPattern(memberId, startDate, endDate),
                statsQueryService.emotionTriggers(memberId, startDate, endDate),
                buildInsightDto(memberId, yearMonth.toString())
        );
    }

    private SummaryStats buildSummary(Long memberId, LocalDate startDate, LocalDate endDate,
                                       List<TrendPoint> trend, List<EmotionShare> dist) {
        int diaryCount = statsQueryService.countDiaries(memberId, startDate, endDate);
        int streak = statsQueryService.computeStreak(memberId);
        String mainEmotion = dist.isEmpty() ? "없음" : dist.get(0).emotion();
        int stability = computeStability(trend);
        return new SummaryStats(diaryCount, mainEmotion, stability, streak);
    }

    /** 감정 점수 표준편차 기반 안정도 (0~100) */
    private int computeStability(List<TrendPoint> trend) {
        if (trend.isEmpty()) return 0;
        double avg = trend.stream().mapToDouble(TrendPoint::score).average().orElse(0);
        double variance = trend.stream().mapToDouble(p -> Math.pow(p.score() - avg, 2)).average().orElse(0);
        double stddev = Math.sqrt(variance);
        return (int) Math.max(0, Math.min(100, Math.round(100 - stddev * 10)));
    }

    private MonthlyInsightDto buildInsightDto(Long memberId, String yearMonth) {
        return monthlyInsightRepository.findByMemberIdAndYearMonth(memberId, yearMonth)
                .map(insight -> {
                    try {
                        List<String> recs = objectMapper.readValue(
                                insight.getRecommendationsJson(),
                                new TypeReference<>() {});
                        return new MonthlyInsightDto(insight.getSummary(), insight.getTrend(), recs);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .orElse(null);
    }
}
