package hwan.project2.service.letter;

import hwan.project2.domain.character.Character;
import hwan.project2.domain.diary.Diary;
import hwan.project2.domain.diary.DiaryEmotion;
import hwan.project2.service.ai.OpenAiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LetterGenerationService {

    private final OpenAiClient openAiClient;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            당신은 '%s'라는 이름의 캐릭터입니다.
            말투: %s
            성격: %s

            오늘 사용자가 일기를 작성했습니다. 사용자의 감정을 깊이 공감하고 위로하는 따뜻한 편지를 작성해주세요.
            편지는 3~4문단으로 구성하고, 위 캐릭터의 말투와 성격을 일관되게 유지하세요.
            사용자의 일기 내용과 감정을 구체적으로 언급하며 공감해주세요.
            편지 마지막 문단에는 내일을 응원하는 따뜻한 한마디를 추가해주세요.
            편지 형식은 자연스러운 편지체로 작성하고, 설명이나 JSON 없이 편지 본문만 출력하세요.
            """;

    private static final String DEFAULT_SYSTEM_PROMPT = """
            당신은 따뜻하고 공감적인 상담사입니다.

            오늘 사용자가 일기를 작성했습니다. 사용자의 감정을 깊이 공감하고 위로하는 따뜻한 편지를 작성해주세요.
            편지는 3~4문단으로 구성하세요.
            사용자의 일기 내용과 감정을 구체적으로 언급하며 공감해주세요.
            편지 마지막 문단에는 내일을 응원하는 따뜻한 한마디를 추가해주세요.
            편지 형식은 자연스러운 편지체로 작성하고, 설명이나 JSON 없이 편지 본문만 출력하세요.
            """;

    public String generate(Diary diary, Character character) {
        String systemPrompt = buildSystemPrompt(character);
        String userPrompt = buildUserPrompt(diary);
        return openAiClient.generateLetter(systemPrompt, userPrompt);
    }

    private String buildSystemPrompt(Character character) {
        if (character == null) {
            return DEFAULT_SYSTEM_PROMPT;
        }
        return SYSTEM_PROMPT_TEMPLATE.formatted(
                character.getName(),
                character.getTone().getDescription(),
                character.getPersonality().getLabel() + " - " + character.getPersonality().getDescription()
        );
    }

    private String buildUserPrompt(Diary diary) {
        String emotionSummary = diary.getEmotions().stream()
                .map(e -> e.getEmotionName() + "(" + e.getScore() + ")")
                .collect(Collectors.joining(", "));

        String keywordSummary = diary.getKeywords().stream()
                .map(k -> k.getWord())
                .collect(Collectors.joining(", "));

        return """
                [오늘의 일기]
                %s

                [분석된 감정]
                %s

                [핵심 키워드]
                %s

                [AI 피드백]
                %s
                """.formatted(
                diary.getContent(),
                emotionSummary.isBlank() ? "분석 중" : emotionSummary,
                keywordSummary.isBlank() ? "없음" : keywordSummary,
                diary.getFeedback() != null ? diary.getFeedback() : ""
        );
    }
}
