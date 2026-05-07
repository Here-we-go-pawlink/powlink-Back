package hwan.project2.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hwan.project2.domain.character.Character;
import hwan.project2.domain.character.repo.CharacterRepository;
import hwan.project2.domain.member.Member;
import hwan.project2.domain.member.repo.MemberRepository;
import hwan.project2.service.diary.DiaryService;
import hwan.project2.service.stats.StatsQueryService;
import hwan.project2.web.dto.chat.ChatMessageDto;
import hwan.project2.web.dto.diary.DiaryCreateRequest;
import hwan.project2.web.dto.stats.StatsResponse.EmotionShare;
import hwan.project2.web.dto.stats.StatsResponse.KeywordStat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_TURNS = 8;
    private static final int COMPRESS_AFTER_TURNS = 4; // 4턴까지 전체, 5턴부터 오래된 메시지 압축
    private final ObjectMapper objectMapper = new ObjectMapper();


    private static final String DIARY_GENERATE_SYSTEM_PROMPT = """
            다음은 사용자가 AI와 나눈 대화입니다.
            이 대화를 바탕으로 사용자의 1인칭 시점 감정 일기를 작성해주세요.
            제목은 오늘 하루를 한 줄로 표현하고, 본문은 감정이 잘 드러나도록 자연스럽게 작성해주세요.
            반드시 아래 JSON 형식으로만 반환하세요.
            {"title": "제목", "content": "본문 내용"}
            """;

    private final OpenAiClient openAiClient;
    private final DiaryService diaryService;
    private final MemberRepository memberRepository;
    private final CharacterRepository characterRepository;
    private final StatsQueryService statsQueryService;

    @Transactional
    public String reply(Long memberId, List<ChatMessageDto> messages) {
        if (messages.size() > MAX_TURNS * 2) {
            return "오늘 이야기를 충분히 나눴어요. 이제 일기로 정리해볼까요?";
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found: " + memberId));

        String currentMonth = YearMonth.now().toString();
        // 첫 번째 메시지(새 세션 시작)일 때만 횟수 차감
        boolean isNewSession = messages.size() == 1;
        if (isNewSession) {
            if (member.isChatLimitExceeded(currentMonth)) {
                return "이번 달 AI 대화 횟수(월 5회)를 모두 사용했어요. 다음 달에 다시 만나요!";
            }
            member.incrementChatUsed(currentMonth);
        }

        List<Map<String, String>> openAiMessages = new ArrayList<>();
        openAiMessages.add(Map.of("role", "system", "content", buildSystemPrompt(memberId)));
        openAiMessages.addAll(buildCompressedMessages(messages));

        return openAiClient.chat(openAiMessages);
    }

    private String buildSystemPrompt(Long memberId) {
        return characterRepository.findByMemberId(memberId)
                .map(c -> String.format(
                        "당신은 사용자의 감정 일기 AI 친구입니다. 이름은 '%s'이고, 성격은 %s, 말투는 %s입니다. " +
                        "사용자의 하루 이야기를 공감하며 들어주고, 감정을 자연스럽게 이끌어내세요. " +
                        "대화는 짧고 따뜻하게 유지하며, 질문은 한 번에 하나만 하세요.",
                        c.getName(), c.getPersonality().getDescription(), c.getTone().getDescription()))
                .orElse("당신은 사용자의 감정 일기 AI 친구입니다. " +
                        "사용자의 하루 이야기를 공감하며 들어주고, 감정을 자연스럽게 이끌어내세요. " +
                        "대화는 짧고 따뜻하게 유지하며, 질문은 한 번에 하나만 하세요.");
    }

    private List<Map<String, String>> buildCompressedMessages(List<ChatMessageDto> messages) {
        List<Map<String, String>> result = new ArrayList<>();
        int threshold = COMPRESS_AFTER_TURNS * 2; // 8 메시지 = 4턴

        if (messages.size() <= threshold) {
            for (ChatMessageDto msg : messages) {
                result.add(toOpenAiMessage(msg));
            }
            return result;
        }

        // 오래된 메시지 요약, 최근 4턴(8개)만 유지
        List<ChatMessageDto> oldMessages = messages.subList(0, messages.size() - threshold);
        List<ChatMessageDto> recentMessages = messages.subList(messages.size() - threshold, messages.size());

        StringBuilder oldConv = new StringBuilder();
        for (ChatMessageDto msg : oldMessages) {
            oldConv.append(msg.role().equals("ai") ? "AI" : "나")
                   .append(": ").append(msg.text()).append("\n");
        }

        String summary = openAiClient.summarizeConversation(oldConv.toString());
        result.add(Map.of("role", "system", "content", "[이전 대화 요약] " + summary));

        for (ChatMessageDto msg : recentMessages) {
            result.add(toOpenAiMessage(msg));
        }
        return result;
    }

    private Map<String, String> toOpenAiMessage(ChatMessageDto msg) {
        return Map.of("role", msg.role().equals("ai") ? "assistant" : "user", "content", msg.text());
    }

    public Long finish(Long memberId, List<ChatMessageDto> messages) {
        List<Map<String, String>> openAiMessages = new ArrayList<>();
        openAiMessages.add(Map.of("role", "system", "content", DIARY_GENERATE_SYSTEM_PROMPT));

        StringBuilder conversation = new StringBuilder();
        for (ChatMessageDto msg : messages) {
            String speaker = msg.role().equals("ai") ? "AI" : "나";
            conversation.append(speaker).append(": ").append(msg.text()).append("\n");
        }
        openAiMessages.add(Map.of("role", "user", "content", conversation.toString()));

        String json = openAiClient.generateDiary(openAiMessages);

        try {
            JsonNode node = objectMapper.readTree(json);
            String title = node.path("title").asText("오늘의 일기");
            String content = node.path("content").asText("");

            DiaryCreateRequest req = new DiaryCreateRequest(
                    title, content, LocalDate.now(), null, null, false, List.of(), List.of()
            );
            return diaryService.createDiary(memberId, req);
        } catch (Exception e) {
            log.error("일기 자동 생성 실패", e);
            throw new RuntimeException("일기 생성에 실패했습니다.");
        }
    }
}
