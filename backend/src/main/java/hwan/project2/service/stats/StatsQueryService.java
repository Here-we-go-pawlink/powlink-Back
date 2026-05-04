package hwan.project2.service.stats;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import hwan.project2.domain.diary.AnalysisStatus;
import hwan.project2.domain.diary.QDiary;
import hwan.project2.domain.diary.QDiaryEmotion;
import hwan.project2.domain.diary.QDiaryKeyword;
import hwan.project2.web.dto.stats.StatsResponse.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StatsQueryService {

    private static final QDiary diary = QDiary.diary;
    private static final QDiaryEmotion emotion = QDiaryEmotion.diaryEmotion;
    private static final QDiaryKeyword keyword = QDiaryKeyword.diaryKeyword;

    private final JPAQueryFactory queryFactory;

    /** 이달 일기 수 */
    public int countDiaries(Long memberId, LocalDate startDate, LocalDate endDate) {
        Long count = queryFactory
                .select(diary.count())
                .from(diary)
                .where(diary.member.id.eq(memberId)
                        .and(diary.diaryDate.between(startDate, endDate))
                        .and(diary.status.eq(AnalysisStatus.COMPLETED)))
                .fetchOne();
        return count != null ? count.intValue() : 0;
    }

    /** 연속 기록 일수 (오늘부터 역순) */
    public int computeStreak(Long memberId) {
        List<LocalDate> dates = queryFactory
                .selectDistinct(diary.diaryDate)
                .from(diary)
                .where(diary.member.id.eq(memberId))
                .orderBy(diary.diaryDate.desc())
                .fetch();

        Set<LocalDate> dateSet = new HashSet<>(dates);
        int streak = 0;
        LocalDate current = LocalDate.now();
        while (dateSet.contains(current)) {
            streak++;
            current = current.minusDays(1);
        }
        return streak;
    }

    /** 날짜별 평균 감정 점수 (0~10 스케일) */
    public List<TrendPoint> emotionTrend(Long memberId, LocalDate startDate, LocalDate endDate) {
        List<Tuple> rows = queryFactory
                .select(diary.diaryDate, emotion.score.avg())
                .from(emotion)
                .join(emotion.diary, diary)
                .where(diary.member.id.eq(memberId)
                        .and(diary.diaryDate.between(startDate, endDate))
                        .and(diary.status.eq(AnalysisStatus.COMPLETED)))
                .groupBy(diary.diaryDate)
                .orderBy(diary.diaryDate.asc())
                .fetch();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M/d");
        return rows.stream()
                .map(t -> {
                    double raw = t.get(1, Double.class) != null ? t.get(1, Double.class) : 0.0;
                    double score = Math.round(raw / 10.0 * 10) / 10.0; // 0~10 스케일, 소수 1자리
                    return new TrendPoint(t.get(diary.diaryDate).format(fmt), score);
                })
                .collect(Collectors.toList());
    }

    /** 감정별 비율 (%) */
    public List<EmotionShare> emotionDistribution(Long memberId, LocalDate startDate, LocalDate endDate) {
        List<Tuple> rows = queryFactory
                .select(emotion.emotionName, emotion.score.sum())
                .from(emotion)
                .join(emotion.diary, diary)
                .where(diary.member.id.eq(memberId)
                        .and(diary.diaryDate.between(startDate, endDate))
                        .and(diary.status.eq(AnalysisStatus.COMPLETED)))
                .groupBy(emotion.emotionName)
                .orderBy(emotion.score.sum().desc())
                .limit(5)
                .fetch();

        long total = rows.stream()
                .mapToLong(t -> t.get(1, Integer.class) != null ? t.get(1, Integer.class) : 0)
                .sum();

        if (total == 0) return List.of();

        return rows.stream()
                .map(t -> {
                    String name = t.get(emotion.emotionName);
                    long sum = t.get(1, Integer.class) != null ? t.get(1, Integer.class) : 0;
                    int pct = (int) Math.round(sum * 100.0 / total);
                    return new EmotionShare(name, pct);
                })
                .collect(Collectors.toList());
    }

    /** 키워드 빈도 Top 12 */
    public List<KeywordStat> keywords(Long memberId, LocalDate startDate, LocalDate endDate) {
        List<Tuple> rows = queryFactory
                .select(keyword.word, keyword.id.count())
                .from(keyword)
                .join(keyword.diary, diary)
                .where(diary.member.id.eq(memberId)
                        .and(diary.diaryDate.between(startDate, endDate))
                        .and(diary.status.eq(AnalysisStatus.COMPLETED)))
                .groupBy(keyword.word)
                .orderBy(keyword.id.count().desc())
                .limit(12)
                .fetch();

        return rows.stream()
                .map(t -> new KeywordStat(t.get(keyword.word), t.get(1, Long.class).intValue()))
                .collect(Collectors.toList());
    }

    /**
     * 요일별 평균 감정 점수 (0~10 스케일)
     * JPQL dayOfWeek(): 1=일, 2=월, 3=화, 4=수, 5=목, 6=금, 7=토
     */
    public List<DowStat> dowPattern(Long memberId, LocalDate startDate, LocalDate endDate) {
        List<Tuple> rows = queryFactory
                .select(diary.diaryDate.dayOfWeek(), emotion.score.avg())
                .from(emotion)
                .join(emotion.diary, diary)
                .where(diary.member.id.eq(memberId)
                        .and(diary.diaryDate.between(startDate, endDate))
                        .and(diary.status.eq(AnalysisStatus.COMPLETED)))
                .groupBy(diary.diaryDate.dayOfWeek())
                .orderBy(diary.diaryDate.dayOfWeek().asc())
                .fetch();

        Map<Integer, Double> scoreByDow = new LinkedHashMap<>();
        for (Tuple t : rows) {
            int dow = t.get(0, Integer.class);
            double avg = t.get(1, Double.class) != null ? t.get(1, Double.class) : 0.0;
            scoreByDow.put(dow, Math.round(avg / 10.0 * 10) / 10.0);
        }

        // 월~일 순서로 반환 (2=월 ~ 7=토, 1=일)
        String[] labels = {"월", "화", "수", "목", "금", "토", "일"};
        int[] jpqlOrder = {2, 3, 4, 5, 6, 7, 1};
        List<DowStat> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            double score = scoreByDow.getOrDefault(jpqlOrder[i], 0.0);
            result.add(new DowStat(labels[i], score));
        }
        return result;
    }

    /** 시간대별 평균 감정 점수 */
    public List<TimeSlotStat> timeSlotPattern(Long memberId, LocalDate startDate, LocalDate endDate) {
        List<Tuple> rows = queryFactory
                .select(diary.createdAt.hour(), emotion.score.avg())
                .from(emotion)
                .join(emotion.diary, diary)
                .where(diary.member.id.eq(memberId)
                        .and(diary.diaryDate.between(startDate, endDate))
                        .and(diary.status.eq(AnalysisStatus.COMPLETED)))
                .groupBy(diary.createdAt.hour())
                .fetch();

        // 시간대별 합산
        double[] slotSum = new double[4]; // 0:아침, 1:오후, 2:저녁, 3:밤
        int[] slotCount = new int[4];

        for (Tuple t : rows) {
            int hour = t.get(0, Integer.class);
            double avg = t.get(1, Double.class) != null ? t.get(1, Double.class) : 0.0;
            int slot = hourToSlot(hour);
            slotSum[slot] += avg;
            slotCount[slot]++;
        }

        String[] slotNames = {"아침", "오후", "저녁", "밤"};
        String[] slotIcons = {"🌅", "☀️", "🌆", "🌙"};
        String[] slotDescs = {"비교적 안정", "집중 부담", "피로 누적", "불안감 증가"};

        List<TimeSlotStat> result = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            double score = slotCount[i] > 0
                    ? Math.round(slotSum[i] / slotCount[i] / 10.0 * 10) / 10.0
                    : 0.0;
            result.add(new TimeSlotStat(slotNames[i], slotIcons[i], score, slotDescs[i]));
        }
        return result;
    }

    /** 감정 트리거: 키워드-감정 공출현 Top 5 */
    public List<TriggerStat> emotionTriggers(Long memberId, LocalDate startDate, LocalDate endDate) {
        List<Tuple> rows = queryFactory
                .select(keyword.word, emotion.emotionName, keyword.id.count())
                .from(keyword)
                .join(emotion).on(keyword.diary.id.eq(emotion.diary.id))
                .join(keyword.diary, diary)
                .where(diary.member.id.eq(memberId)
                        .and(diary.diaryDate.between(startDate, endDate))
                        .and(diary.status.eq(AnalysisStatus.COMPLETED)))
                .groupBy(keyword.word, emotion.emotionName)
                .orderBy(keyword.id.count().desc())
                .limit(5)
                .fetch();

        return rows.stream()
                .map(t -> new TriggerStat(
                        t.get(keyword.word),
                        t.get(emotion.emotionName),
                        t.get(2, Long.class)))
                .collect(Collectors.toList());
    }

    /** 월간 AI 인사이트 생성용: 이달 감정/키워드 요약 텍스트 */
    public String buildMonthSummaryText(Long memberId, LocalDate startDate, LocalDate endDate) {
        List<EmotionShare> dist = emotionDistribution(memberId, startDate, endDate);
        List<KeywordStat> kws = keywords(memberId, startDate, endDate);
        List<DowStat> dow = dowPattern(memberId, startDate, endDate);
        int count = countDiaries(memberId, startDate, endDate);

        StringBuilder sb = new StringBuilder();
        sb.append("이번 달 일기 수: ").append(count).append("편\n\n");
        sb.append("[감정 분포]\n");
        dist.forEach(e -> sb.append(e.emotion()).append(" ").append(e.percentage()).append("%\n"));
        sb.append("\n[주요 키워드]\n");
        kws.stream().limit(8).forEach(k -> sb.append(k.text()).append(" (").append(k.count()).append("회)\n"));
        sb.append("\n[요일별 감정 점수]\n");
        dow.forEach(d -> sb.append(d.day()).append("요일: ").append(d.score()).append("점\n"));
        return sb.toString();
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────────────────

    private int hourToSlot(int hour) {
        if (hour >= 6 && hour < 12) return 0;  // 아침
        if (hour >= 12 && hour < 18) return 1; // 오후
        if (hour >= 18 && hour < 22) return 2; // 저녁
        return 3;                               // 밤 (22~5)
    }
}
