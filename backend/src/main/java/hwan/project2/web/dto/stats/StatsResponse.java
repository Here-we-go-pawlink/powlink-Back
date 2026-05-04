package hwan.project2.web.dto.stats;

import java.util.List;

public record StatsResponse(
        SummaryStats summary,
        List<TrendPoint> emotionTrend,
        List<EmotionShare> emotionDistribution,
        List<KeywordStat> keywords,
        List<DowStat> dowPattern,
        List<TimeSlotStat> timeSlotPattern,
        List<TriggerStat> emotionTriggers,
        MonthlyInsightDto aiInsights         // null if not yet generated
) {
    public record SummaryStats(
            int diaryCount,
            String mainEmotion,
            int stabilityScore,
            int streak
    ) {}

    /** 날짜별 평균 감정 점수 (0~10 스케일) */
    public record TrendPoint(String date, double score) {}

    /** 감정별 비율 */
    public record EmotionShare(String emotion, int percentage) {}

    /** 키워드 빈도 */
    public record KeywordStat(String text, int count) {}

    /** 요일별 평균 감정 점수 */
    public record DowStat(String day, double score) {}

    /** 시간대별 평균 감정 점수 */
    public record TimeSlotStat(String slot, String icon, double score, String desc) {}

    /** 키워드-감정 트리거 연관 */
    public record TriggerStat(String keyword, String emotion, long count) {}

    /** AI 월간 인사이트 */
    public record MonthlyInsightDto(
            String summary,
            String trend,
            List<String> recommendations
    ) {}
}
