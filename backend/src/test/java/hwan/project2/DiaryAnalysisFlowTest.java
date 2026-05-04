package hwan.project2;

import hwan.project2.domain.diary.AnalysisStatus;
import hwan.project2.domain.diary.Diary;
import hwan.project2.domain.diary.repo.DiaryRepository;
import hwan.project2.service.ai.OpenAiClient;
import hwan.project2.service.ai.dto.AnalysisResult;
import hwan.project2.service.auth.AuthService;
import hwan.project2.service.diary.DiaryService;
import hwan.project2.web.dto.LoginRequest;
import hwan.project2.web.dto.SignupRequest;
import hwan.project2.web.dto.diary.DiaryCreateRequest;
import hwan.project2.web.dto.diary.DiaryResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
class DiaryAnalysisFlowTest {

    @Autowired AuthService authService;
    @Autowired DiaryService diaryService;
    @Autowired DiaryRepository diaryRepository;

    @MockitoBean StringRedisTemplate redis;
    @MockitoBean OpenAiClient openAiClient;

    private final ConcurrentHashMap<String, String> fakeRedis = new ConcurrentHashMap<>();

    private Long memberId;
    private Long diaryId;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        fakeRedis.clear();

        // Redis → 인메모리 Map으로 대체
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

        // OpenAI 응답 Mock
        when(openAiClient.analyze(anyString(), anyString()))
                .thenReturn(new AnalysisResult(
                        List.of(
                                new AnalysisResult.EmotionData("기쁨", 85),
                                new AnalysisResult.EmotionData("설렘", 60)
                        ),
                        List.of("여행", "친구", "행복"),
                        "오늘 정말 즐거운 하루였겠다! 그 기억을 소중히 간직해봐.",
                        List.of(
                                new AnalysisResult.Recommendation("action", "친구에게 감사 메시지 보내기"),
                                new AnalysisResult.Recommendation("action", "오늘의 기억을 사진 앨범으로 정리하기"),
                                new AnalysisResult.Recommendation("music", "IU - 좋은 날")
                        )
                ));
    }

    @AfterEach
    void tearDown() {
        if (diaryId != null) diaryRepository.deleteById(diaryId);
    }

    @Test
    @DisplayName("회원가입 → 로그인 → 일기 작성 → AI 분석 완료 → 결과 조회")
    void 일기작성_AI분석_전체흐름() {

        // ── 1. 회원가입 ───────────────────────────────────────────────────────────
        memberId = authService.signup(
                new SignupRequest("테스터", "flowtest@example.com", "password123", null));
        System.out.println("\n✅ [1] 회원가입 성공 / memberId: " + memberId);

        // ── 2. 로그인 ─────────────────────────────────────────────────────────────
        var tokenResponse = authService.login(
                new LoginRequest("flowtest@example.com", "password123"));
        assertThat(tokenResponse.accessToken()).isNotBlank();
        assertThat(tokenResponse.refreshToken()).isNotBlank();
        System.out.println("✅ [2] 로그인 성공 / accessToken 발급 확인");

        // ── 3. 일기 작성 ──────────────────────────────────────────────────────────
        DiaryCreateRequest req = new DiaryCreateRequest(
                "오늘은 정말 즐거운 하루였다",
                "친구들과 제주도 여행을 다녀왔다. 바다도 보고 맛있는 것도 먹고 정말 행복했다.",
                LocalDate.of(2026, 4, 8),
                null,
                null,
                false,
                null
        );
        diaryId = diaryService.createDiary(memberId, req);
        System.out.println("✅ [3] 일기 작성 성공 / diaryId: " + diaryId);

        // 최초 상태: PENDING
        Diary created = diaryRepository.findById(diaryId).orElseThrow();
        assertThat(created.getStatus()).isEqualTo(AnalysisStatus.PENDING);
        System.out.println("✅ [4] 초기 상태 PENDING 확인");

        // ── 4. AI 분석 완료 대기 (비동기) ─────────────────────────────────────────
        System.out.println("⏳ [5] AI 분석 대기 중 (최대 10초)...");
        await().atMost(10, SECONDS).untilAsserted(() -> {
            Diary diary = diaryRepository.findById(diaryId).orElseThrow();
            assertThat(diary.getStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        });
        System.out.println("✅ [5] AI 분석 완료 (COMPLETED)");

        // ── 5. 분석 결과 조회 및 검증 ─────────────────────────────────────────────
        DiaryResponse result = diaryService.getDiary(memberId, diaryId);

        System.out.println("\n📋 [6] AI 분석 결과:");
        System.out.println("   상태:    " + result.status());
        System.out.println("   피드백:  " + result.feedback());
        System.out.println("   감정:    " + result.emotions());
        System.out.println("   키워드:  " + result.keywords());
        System.out.println("   추천:    " + result.recommendations());

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.feedback()).isEqualTo("오늘 정말 즐거운 하루였겠다! 그 기억을 소중히 간직해봐.");
        assertThat(result.emotions()).hasSize(2);
        assertThat(result.emotions().get(0).emotionName()).isEqualTo("기쁨");
        assertThat(result.emotions().get(0).score()).isEqualTo(85);
        assertThat(result.keywords()).containsExactlyInAnyOrder("여행", "친구", "행복");
        assertThat(result.recommendations()).hasSize(3);
        assertThat(result.recommendations().stream().anyMatch(r -> r.content().contains("IU"))).isTrue();

        // ── 6. OpenAI 정확히 1회 호출됐는지 확인 (Race Condition 없음) ─────────────
        verify(openAiClient, times(1)).analyze(anyString(), anyString());
        System.out.println("\n✅ [7] OpenAI 정확히 1회 호출 확인 (Race Condition 없음)");
        System.out.println("✅ 전체 흐름 테스트 통과!");
    }
}
