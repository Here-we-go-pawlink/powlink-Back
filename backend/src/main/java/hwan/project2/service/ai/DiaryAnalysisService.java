package hwan.project2.service.ai;

import hwan.project2.domain.character.Character;
import hwan.project2.domain.character.repo.CharacterRepository;
import hwan.project2.domain.diary.AnalysisStatus;
import hwan.project2.domain.diary.Diary;
import hwan.project2.domain.diary.repo.DiaryRepository;
import hwan.project2.service.ai.dto.AnalysisResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryAnalysisService {

    private final DiaryRepository diaryRepository;
    private final CharacterRepository characterRepository;
    private final OpenAiClient openAiClient;
    private final ApplicationEventPublisher eventPublisher;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            당신은 사용자의 감정을 분석하는 AI 심리 분석 전문가입니다.
            사용자의 일기와 현재 상황 정보를 바탕으로 세 가지 작업을 수행하세요.

            [캐릭터 설정]
            당신은 '%s'라는 이름의 캐릭터입니다.
            말투: %s
            성격: %s

            [작업 1: 감정 및 키워드 추출]
            아래 감정 목록 중 일기에 나타난 감정을 최대 3개 선택하고, 각각 0~100 사이 강도 점수를 부여하세요.
            감정 목록: 기쁨, 슬픔, 분노, 불안, 설렘, 우울, 피로, 외로움, 후회, 감사, 평온
            감정 유발의 핵심 원인이 되는 명사 키워드를 최대 5개 추출하세요. (예: 면접, 진로, 친구, 업무)

            [작업 2: 즉각 피드백]
            위 캐릭터의 말투와 성격으로, 사용자의 감정에 공감하는 짧은 한마디(1~2문장)를 작성하세요.

            [작업 3: 상황 맞춤 추천]
            추출한 감정과 현재 상황(시각, 요일)을 고려하여 추천을 제시하세요.
            - 사용자 선호 음악 장르: %s → 이 장르에서 어울리는 곡 1가지 추천
            - 사용자 선호 활동 유형: %s → 지금 실천 가능한 행동 2가지 추천
            추천은 구체적이고 현실적으로 작성하세요.

            반드시 아래 JSON 형식으로만 반환하세요. 설명 텍스트 없이 JSON만 출력하세요.
            {
              "emotions": [{"name": "슬픔", "score": 75}],
              "keywords": ["면접", "진로"],
              "oneLiner": "오늘 정말 힘들었겠다. 그래도 여기까지 잘 버텨왔어.",
              "recommendations": [
                {"type": "action", "content": "따뜻한 물로 샤워하고 좋아하는 영상 보기"},
                {"type": "action", "content": "자기 전 오늘 잘한 일 1가지 적어보기"},
                {"type": "music", "content": "Frank Ocean - Self Control"}
              ]
            }
            """;

    private static final String DEFAULT_SYSTEM_PROMPT = """
            당신은 사용자의 감정을 분석하는 AI 심리 분석 전문가입니다.
            사용자의 일기와 현재 상황 정보를 바탕으로 세 가지 작업을 수행하세요.

            [작업 1: 감정 및 키워드 추출]
            아래 감정 목록 중 일기에 나타난 감정을 최대 3개 선택하고, 각각 0~100 사이 강도 점수를 부여하세요.
            감정 목록: 기쁨, 슬픔, 분노, 불안, 설렘, 우울, 피로, 외로움, 후회, 감사, 평온
            감정 유발의 핵심 원인이 되는 명사 키워드를 최대 5개 추출하세요. (예: 면접, 진로, 친구, 업무)

            [작업 2: 즉각 피드백]
            따뜻하고 공감적인 말투로, 사용자의 감정에 공감하는 짧은 한마디(1~2문장)를 작성하세요.

            [작업 3: 상황 맞춤 추천]
            추출한 감정과 현재 상황(시각, 요일)을 고려하여, 지금 실천 가능한 행동 추천 2가지와 음악 추천 1가지를 제시하세요.
            추천은 구체적이고 현실적으로 작성하세요.

            반드시 아래 JSON 형식으로만 반환하세요. 설명 텍스트 없이 JSON만 출력하세요.
            {
              "emotions": [{"name": "슬픔", "score": 75}],
              "keywords": ["면접", "진로"],
              "oneLiner": "오늘 정말 힘들었겠다. 그래도 여기까지 잘 버텨왔어.",
              "recommendations": [
                {"type": "action", "content": "따뜻한 물로 샤워하고 좋아하는 영상 보기"},
                {"type": "action", "content": "자기 전 오늘 잘한 일 1가지 적어보기"},
                {"type": "music", "content": "Frank Ocean - Self Control"}
              ]
            }
            """;

    @Async("analysisExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDiaryCreated(DiaryCreatedEvent event) {
        runAnalysis(event.diaryId());
    }

    @Async("analysisExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retryAnalyze(Long diaryId) {
        log.info("분석 재시도: diaryId={}", diaryId);
        runAnalysis(diaryId);
    }

    private void runAnalysis(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId).orElse(null);
        if (diary == null) {
            log.warn("분석 대상 일기를 찾을 수 없음: diaryId={}", diaryId);
            return;
        }
        if (diary.getStatus() == AnalysisStatus.COMPLETED) {
            log.info("이미 분석 완료된 일기 - 스킵: diaryId={}", diaryId);
            return;
        }
        if (diary.getStatus() == AnalysisStatus.ANALYZING) {
            log.warn("현재 분석 중인 일기 - 스킵: diaryId={}", diaryId);
            return;
        }

        diary.startAnalyzing();

        try {
            Long memberId = diary.getMember().getId();
            String systemPrompt = buildSystemPrompt(memberId);
            String userPrompt = buildUserPrompt(diary);
            AnalysisResult result = openAiClient.analyze(systemPrompt, userPrompt);
            diary.completeAnalysis(result);
            log.info("일기 분석 완료: diaryId={}", diaryId);
            eventPublisher.publishEvent(new DiaryAnalysisCompletedEvent(diaryId, diary.getMember().getId()));
        } catch (Exception e) {
            log.error("일기 분석 실패: diaryId={}", diaryId, e);
            diary.failAnalysis();
        }
    }

    private String buildSystemPrompt(Long memberId) {
        return characterRepository.findByMemberId(memberId)
                .map(c -> SYSTEM_PROMPT_TEMPLATE.formatted(
                        c.getName(),
                        c.getTone().getDescription(),
                        c.getPersonality().getLabel() + " - " + c.getPersonality().getDescription(),
                        c.getMusicGenre().getLabel(),
                        c.getActivityType().getLabel()
                ))
                .orElse(DEFAULT_SYSTEM_PROMPT);
    }

    private String buildUserPrompt(Diary diary) {
        // 분석 시점이 아닌 실제 일기 작성 시각 기준
        LocalDateTime createdAt = diary.getCreatedAt();
        int hour = createdAt.getHour();
        String timeSlot = resolveTimeSlot(hour);
        String dayOfWeek = diary.getDiaryDate()
                .getDayOfWeek()
                .getDisplayName(TextStyle.FULL, Locale.KOREAN);
        String weather = diary.getWeather() != null ? diary.getWeather().name() : "정보 없음";

        return """
                [일기]
                %s

                [일기 작성 상황]
                - 작성 시각: %d시 (%s)
                - 요일: %s
                - 날씨: %s
                """.formatted(diary.getContent(), hour, timeSlot, dayOfWeek, weather);
    }

    private String resolveTimeSlot(int hour) {
        if (hour >= 0 && hour < 5) return "새벽";
        if (hour < 9) return "아침";
        if (hour < 13) return "오전";
        if (hour < 18) return "오후";
        if (hour < 21) return "저녁";
        return "밤";
    }
}
