package hwan.project2.service.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hwan.project2.domain.diary.ActionType;
import hwan.project2.domain.diary.Diary;
import hwan.project2.domain.diary.DiaryKeyword;
import hwan.project2.domain.diary.WeeklyReport;
import hwan.project2.domain.diary.repo.DiaryRepository;
import hwan.project2.domain.diary.repo.WeeklyReportRepository;
import hwan.project2.domain.letter.Letter;
import hwan.project2.domain.letter.repo.LetterRepository;
import hwan.project2.domain.member.Member;
import hwan.project2.service.ai.OpenAiClient;
import hwan.project2.service.notification.WeeklyReportCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class WeeklyReportGenerationService {

    private static final int MIN_DIARY_COUNT = 3;

    private static final String SYSTEM_PROMPT = """
            당신은 사용자의 한 주간 감정 일기를 분석하는 AI입니다.
            제공된 일기들을 바탕으로 이번 주를 분석하고, 반드시 아래 JSON 형식으로만 반환하세요.
            {
              "weeklySummary": "이번 주 총평 (2~3문장)",
              "dominantEmotion": "가장 지배적인 감정 1개 (예: 불안, 기쁨, 슬픔)",
              "recommendedAction": "MUSIC 또는 MOVIE 또는 ACTIVITY 또는 REST 중 하나",
              "recommendationMessage": "맞춤형 추천 메시지 1~2문장",
              "searchKeyword": "추천 콘텐츠 검색 키워드 (예: Frank Ocean 잔잔한 곡)"
            }
            """;

    private final DiaryRepository diaryRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final LetterRepository letterRepository;
    private final OpenAiClient openAiClient;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void generate(Long memberId, LocalDate startDate, LocalDate endDate) {
        if (weeklyReportRepository.existsByMemberIdAndStartDate(memberId, startDate)) {
            log.debug("주간 리포트 이미 존재: memberId={}, startDate={}", memberId, startDate);
            return;
        }

        List<Diary> diaries = diaryRepository.findCompletedByMemberAndDateRange(memberId, startDate, endDate);
        if (diaries.size() < MIN_DIARY_COUNT) {
            return;
        }

        doGenerate(memberId, startDate, endDate, diaries);
    }

    /** dev/테스트 전용: 일기 1개만 있어도 생성, 기존 리포트 덮어씀 */
    public void generateForDev(Long memberId, LocalDate startDate, LocalDate endDate) {
        weeklyReportRepository.deleteByMemberIdAndStartDate(memberId, startDate);

        List<Diary> diaries = diaryRepository.findCompletedByMemberAndDateRange(memberId, startDate, endDate);
        if (diaries.isEmpty()) {
            log.warn("주간 리포트 생성 불가 - 해당 기간 완료된 일기 없음: memberId={}", memberId);
            return;
        }

        doGenerate(memberId, startDate, endDate, diaries);
    }

    private void doGenerate(Long memberId, LocalDate startDate, LocalDate endDate, List<Diary> diaries) {

        Member member = diaries.get(0).getMember();
        if (member == null) {
            log.error("주간 리포트 생성 실패 - 멤버를 찾을 수 없음: memberId={}", memberId);
            return;
        }

        try {
            String userContent = buildUserContent(diaries);
            String json = openAiClient.generateWeeklyReport(SYSTEM_PROMPT, userContent);
            JsonNode node = objectMapper.readTree(json);

            ActionType recommendedAction;
            try {
                recommendedAction = ActionType.valueOf(node.path("recommendedAction").asText("REST"));
            } catch (IllegalArgumentException e) {
                recommendedAction = ActionType.REST;
            }

            WeeklyReport report = WeeklyReport.create(
                    member, startDate, endDate,
                    node.path("weeklySummary").asText(),
                    node.path("dominantEmotion").asText(),
                    recommendedAction,
                    node.path("recommendationMessage").asText(),
                    node.path("searchKeyword").asText()
            );
            weeklyReportRepository.save(report);

            LocalDateTime deliverAt = endDate.plusDays(1).atTime(9, 0); // 월요일 09:00
            Letter letter = Letter.ofWeeklyReport(member, buildLetterContent(report), deliverAt);
            letterRepository.save(letter);
            eventPublisher.publishEvent(new WeeklyReportCreatedEvent(memberId));

            log.info("주간 리포트 생성 완료: memberId={}, period={} ~ {}", memberId, startDate, endDate);
        } catch (Exception e) {
            log.error("주간 리포트 생성 실패: memberId={}", memberId, e);
        }
    }

    private String buildUserContent(List<Diary> diaries) {
        StringBuilder sb = new StringBuilder();
        for (Diary diary : diaries) {
            String emotions = diary.getEmotions().stream()
                    .map(e -> e.getEmotionName() + "(" + e.getScore() + ")")
                    .collect(Collectors.joining(", "));
            String keywords = diary.getKeywords().stream()
                    .map(DiaryKeyword::getWord)
                    .collect(Collectors.joining(", "));

            sb.append("[").append(diary.getDiaryDate()).append("]\n")
              .append("내용: ").append(diary.getContent()).append("\n")
              .append("감정: ").append(emotions.isBlank() ? "없음" : emotions).append("\n")
              .append("키워드: ").append(keywords.isBlank() ? "없음" : keywords).append("\n\n");
        }
        return sb.toString();
    }

    private String buildLetterContent(WeeklyReport report) {
        return """
                안녕하세요, 이번 주도 일기를 써주셨군요.

                %s

                이번 주 가장 많이 느낀 감정은 '%s'이었어요.

                %s

                한 주 동안 정말 수고하셨어요. 다음 주도 응원할게요.
                """.formatted(
                report.getWeeklySummary(),
                report.getDominantEmotion(),
                report.getRecommendationMessage()
        );
    }
}
