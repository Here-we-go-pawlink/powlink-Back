package hwan.project2.service.stats;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hwan.project2.domain.member.Member;
import hwan.project2.domain.member.repo.MemberRepository;
import hwan.project2.domain.stats.MonthlyInsight;
import hwan.project2.domain.stats.repo.MonthlyInsightRepository;
import hwan.project2.service.ai.OpenAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MonthlyInsightGenerationService {

    private final MonthlyInsightRepository monthlyInsightRepository;
    private final MemberRepository memberRepository;
    private final StatsQueryService statsQueryService;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void generate(Long memberId, YearMonth yearMonth) {
        String ym = yearMonth.toString();
        if (monthlyInsightRepository.existsByMemberIdAndYearMonth(memberId, ym)) {
            log.debug("월간 인사이트 이미 존재: memberId={}, yearMonth={}", memberId, ym);
            return;
        }

        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null) return;

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        int diaryCount = statsQueryService.countDiaries(memberId, startDate, endDate);
        if (diaryCount < 3) {
            log.debug("일기 수 부족으로 월간 인사이트 생성 생략: memberId={}, count={}", memberId, diaryCount);
            return;
        }

        try {
            String summaryText = statsQueryService.buildMonthSummaryText(memberId, startDate, endDate);
            String json = openAiClient.generateMonthlyInsight(summaryText);
            JsonNode node = objectMapper.readTree(json);

            List<String> recommendations = new ArrayList<>();
            node.path("recommendations").forEach(r -> recommendations.add(r.asText()));
            String recommendationsJson = objectMapper.writeValueAsString(recommendations);

            MonthlyInsight insight = MonthlyInsight.create(
                    member, ym,
                    node.path("summary").asText(),
                    node.path("trend").asText(),
                    recommendationsJson
            );
            monthlyInsightRepository.save(insight);
            log.info("월간 인사이트 생성 완료: memberId={}, yearMonth={}", memberId, ym);
        } catch (Exception e) {
            log.error("월간 인사이트 생성 실패: memberId={}", memberId, e);
        }
    }
}
