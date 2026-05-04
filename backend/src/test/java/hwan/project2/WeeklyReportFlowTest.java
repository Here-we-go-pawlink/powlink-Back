package hwan.project2;

import hwan.project2.domain.diary.AnalysisStatus;
import hwan.project2.domain.diary.Diary;
import hwan.project2.domain.diary.repo.DiaryRepository;
import hwan.project2.domain.diary.repo.WeeklyReportRepository;
import hwan.project2.domain.letter.Letter;
import hwan.project2.domain.letter.LetterType;
import hwan.project2.domain.letter.repo.LetterRepository;
import hwan.project2.service.ai.OpenAiClient;
import hwan.project2.service.ai.dto.AnalysisResult;
import hwan.project2.service.auth.AuthService;
import hwan.project2.service.diary.DiaryService;
import hwan.project2.service.report.WeeklyReportGenerationService;
import hwan.project2.web.dto.SignupRequest;
import hwan.project2.web.dto.diary.DiaryCreateRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
class WeeklyReportFlowTest {

    @Autowired AuthService authService;
    @Autowired DiaryService diaryService;
    @Autowired DiaryRepository diaryRepository;
    @Autowired WeeklyReportRepository weeklyReportRepository;
    @Autowired LetterRepository letterRepository;
    @Autowired WeeklyReportGenerationService weeklyReportGenerationService;

    @MockitoBean StringRedisTemplate redis;
    @MockitoBean OpenAiClient openAiClient;

    private final ConcurrentHashMap<String, String> fakeRedis = new ConcurrentHashMap<>();
    private Long memberId;
    private final List<Long> diaryIds = new java.util.ArrayList<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        fakeRedis.clear();
        diaryIds.clear();

        // Redis → 인메모리 Map
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(redis.hasKey(anyString()))
                .thenAnswer(inv -> fakeRedis.containsKey(inv.getArgument(0).toString()));
        when(redis.delete(anyString()))
                .thenAnswer(inv -> { fakeRedis.remove(inv.getArgument(0).toString()); return true; });
        doAnswer(inv -> { fakeRedis.put(inv.getArgument(0), inv.getArgument(1)); return null; })
                .when(valueOps).set(anyString(), anyString(), any(Duration.class));
        when(valueOps.get(anyString()))
                .thenAnswer(inv -> fakeRedis.get(inv.getArgument(0).toString()));

        // OpenAI 분석 Mock
        when(openAiClient.analyze(anyString(), anyString()))
                .thenReturn(new AnalysisResult(
                        List.of(new AnalysisResult.EmotionData("불안", 70),
                                new AnalysisResult.EmotionData("피로", 50)),
                        List.of("직장", "스트레스"),
                        "힘든 한 주였지만 잘 버텨냈어요.",
                        List.of(new AnalysisResult.Recommendation("music", "잔잔한 재즈"))
                ));

        // OpenAI 주간 리포트 Mock
        when(openAiClient.generateWeeklyReport(anyString(), anyString()))
                .thenReturn("""
                        {
                          "weeklySummary": "이번 주는 직장에서 스트레스가 많았던 한 주였습니다. 불안과 피로가 반복되었지만 잘 견뎌냈습니다.",
                          "dominantEmotion": "불안",
                          "recommendedAction": "MUSIC",
                          "recommendationMessage": "주말에는 잔잔한 재즈 음악을 들으며 충분히 쉬어보세요.",
                          "searchKeyword": "재즈 힐링 플레이리스트"
                        }
                        """);
    }

    @AfterEach
    void tearDown() {
        diaryIds.forEach(id -> diaryRepository.findById(id).ifPresent(diaryRepository::delete));
        if (memberId != null) {
            weeklyReportRepository.findAll().stream()
                    .filter(r -> r.getMember().getId().equals(memberId))
                    .forEach(weeklyReportRepository::delete);
            letterRepository.findAll().stream()
                    .filter(l -> l.getMember().getId().equals(memberId))
                    .forEach(letterRepository::delete);
        }
    }

    @Test
    @DisplayName("이번 주 일기 3편 이상 → 주간 리포트 + 편지함 생성")
    void 주간리포트_전체흐름() {
        // ── 1. 회원가입 ───────────────────────────────────────────────
        memberId = authService.signup(
                new SignupRequest("리포트테스터", "reporttest@example.com", "password123", null));
        System.out.println("\n✅ [1] 회원가입 성공 / memberId: " + memberId);

        // ── 2. 이번 주 일기 3편 작성 (월~수) ─────────────────────────
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        for (int i = 0; i < 3; i++) {
            Long id = diaryService.createDiary(memberId, new DiaryCreateRequest(
                    "힘든 하루 " + (i + 1),
                    "직장에서 스트레스를 많이 받았다. 피곤하고 불안했다.",
                    monday.plusDays(i),
                    null, null, false, null
            ));
            diaryIds.add(id);
        }
        System.out.println("✅ [2] 일기 3편 작성 완료: " + monday + " ~ " + monday.plusDays(2));

        // ── 3. AI 분석 완료 대기 ──────────────────────────────────────
        System.out.println("⏳ [3] AI 분석 대기 중 (최대 15초)...");
        await().atMost(15, SECONDS).untilAsserted(() -> {
            long completedCount = diaryIds.stream()
                    .map(id -> diaryRepository.findById(id).orElseThrow())
                    .filter(d -> d.getStatus() == AnalysisStatus.COMPLETED)
                    .count();
            assertThat(completedCount).isEqualTo(3);
        });
        System.out.println("✅ [3] 일기 3편 모두 COMPLETED");

        // ── 4. 주간 리포트 생성 ───────────────────────────────────────
        LocalDate startDate = monday;
        LocalDate endDate = monday.with(DayOfWeek.SUNDAY);
        weeklyReportGenerationService.generate(memberId, startDate, endDate);
        System.out.println("✅ [4] 주간 리포트 생성 호출 완료");

        // ── 5. WeeklyReport 저장 확인 ─────────────────────────────────
        boolean reportExists = weeklyReportRepository.existsByMemberIdAndStartDate(memberId, startDate);
        assertThat(reportExists).isTrue();
        System.out.println("✅ [5] WeeklyReport DB 저장 확인");

        // ── 6. Letter(WEEKLY_REPORT) 생성 확인 ───────────────────────
        LocalDateTime afterNow = endDate.plusDays(1).atTime(8, 59); // deliverAt 직전
        List<Letter> letters = letterRepository.findAll().stream()
                .filter(l -> l.getMember().getId().equals(memberId))
                .filter(l -> l.getType() == LetterType.WEEKLY_REPORT)
                .toList();
        assertThat(letters).hasSize(1);

        Letter letter = letters.get(0);
        assertThat(letter.getDeliverAt()).isEqualTo(endDate.plusDays(1).atTime(9, 0));
        assertThat(letter.isRead()).isFalse();
        assertThat(letter.getContent()).contains("불안");
        System.out.println("✅ [6] Letter(WEEKLY_REPORT) 생성 확인 / deliverAt: " + letter.getDeliverAt());

        // ── 7. 중복 생성 방지 확인 ────────────────────────────────────
        weeklyReportGenerationService.generate(memberId, startDate, endDate);
        long reportCount = weeklyReportRepository.findAll().stream()
                .filter(r -> r.getMember().getId().equals(memberId))
                .count();
        assertThat(reportCount).isEqualTo(1);
        System.out.println("✅ [7] 중복 생성 방지 확인");

        System.out.println("✅ 주간 리포트 전체 흐름 테스트 통과!");
    }
}
