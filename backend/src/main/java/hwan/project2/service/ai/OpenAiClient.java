package hwan.project2.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import hwan.project2.service.ai.dto.AnalysisResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model.analysis}")
    private String analysisModel;

    @Value("${openai.model.chat}")
    private String chatModel;

    @Value("${openai.model.diary-generate}")
    private String diaryGenerateModel;

    @Value("${openai.model.letter}")
    private String letterModel;

    @Value("${openai.model.report}")
    private String reportModel;

    public OpenAiClient(@Value("${openai.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public String chat(List<Map<String, String>> messages) {
        return callCompletions(chatModel, messages, false);
    }

    public String generateDiary(List<Map<String, String>> messages) {
        return callCompletions(diaryGenerateModel, messages, true);
    }

    public String generateMonthlyInsight(String userContent) {
        String systemPrompt = """
                당신은 사용자의 한 달간 감정 일기를 분석하는 AI입니다.
                제공된 월간 통계를 바탕으로 분석하고, 반드시 아래 JSON 형식으로만 반환하세요.
                {
                  "summary": "이번 달 총평 (2~3문장, 주요 감정과 패턴 위주)",
                  "trend": "감정 변화 흐름 설명 (1~2문장)",
                  "recommendations": ["실천 가능한 제안 1", "실천 가능한 제안 2", "실천 가능한 제안 3"]
                }
                """;
        return callCompletions(reportModel, List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userContent)
        ), true);
    }

    public String generateWeeklyReport(String systemPrompt, String userContent) {
        return callCompletions(reportModel, List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userContent)
        ), true);
    }

    public String summarizeConversation(String conversationText) {
        return callCompletions(chatModel, List.of(
                Map.of("role", "system", "content",
                        "다음은 사용자와 AI 감정 일기 도우미의 대화입니다. 핵심 감정, 사건, 언급된 인물을 중심으로 2~3문장으로 요약해주세요."),
                Map.of("role", "user", "content", conversationText)
        ), false);
    }

    public String generateLetter(String systemPrompt, String userContent) {
        return callCompletions(letterModel, List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userContent)
        ), false);
    }

    private String callCompletions(String model, List<Map<String, String>> messages, boolean jsonMode) {
        var bodyBuilder = new java.util.LinkedHashMap<String, Object>();
        bodyBuilder.put("model", model);
        bodyBuilder.put("messages", messages);
        if (jsonMode) bodyBuilder.put("response_format", Map.of("type", "json_object"));

        String responseBody = restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(bodyBuilder)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.has("error")) {
                throw new RuntimeException("OpenAI 오류: " + root.path("error").path("message").asText());
            }
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("OpenAI 응답 파싱 실패", e);
        }
    }

    public AnalysisResult analyze(String systemPrompt, String userPrompt) {
        Map<String, Object> requestBody = Map.of(
                "model", analysisModel,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        String responseBody = restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return parseAnalysisResult(responseBody);
    }

    private AnalysisResult parseAnalysisResult(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // OpenAI 에러 응답 처리 (e.g. rate limit, invalid key)
            if (root.has("error")) {
                String errorMsg = root.path("error").path("message").asText();
                log.error("OpenAI API 에러: {}", errorMsg);
                throw new RuntimeException("OpenAI API 에러: " + errorMsg);
            }

            JsonNode choices = root.path("choices");
            if (choices.isEmpty()) {
                throw new RuntimeException("OpenAI 응답에 choices가 없습니다: " + responseBody);
            }

            String content = choices.get(0).path("message").path("content").asText();
            if (content.isBlank()) {
                throw new RuntimeException("OpenAI 응답 content가 비어있습니다");
            }

            return objectMapper.readValue(content, AnalysisResult.class);
        } catch (JsonProcessingException e) {
            log.error("OpenAI 응답 파싱 실패: {}", responseBody, e);
            throw new RuntimeException("AI 분석 응답 파싱에 실패했습니다.", e);
        }
    }
}
