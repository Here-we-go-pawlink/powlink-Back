package hwan.project2.domain.diary;

import hwan.project2.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "weekly_report_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // 리포트 대상 기간
    private LocalDate startDate; // 예: 2026-03-16 (월)
    private LocalDate endDate;   // 예: 2026-03-22 (일)

    @Column(columnDefinition = "TEXT")
    private String weeklySummary; // AI가 요약한 이번 주 총평 (예: "진로 고민으로 불안감이 높았던 한 주였습니다...")

    private String dominantEmotion; // 이번 주를 지배한 핵심 감정 1개 (예: "불안")

    // 맞춤형 추천 영역
    @Enumerated(EnumType.STRING)
    private ActionType recommendedAction; // [MUSIC, MOVIE, ACTIVITY, REST]

    @Column(columnDefinition = "TEXT")
    private String recommendationMessage; // 예: "주말에는 코딩을 잠시 멈추고, R&B 음악과 함께 푹 쉬어보세요."

    private String searchKeyword; // 외부 API 검색용 (예: "Frank Ocean 잔잔한 곡")

    private boolean isActionCompleted; // 사용자가 추천 행동을 수행했는지 (피드백 루프용)

    private LocalDateTime createdAt; // 배치 작업이 실행되어 리포트가 생성된 시간

    public static WeeklyReport create(Member member, LocalDate startDate, LocalDate endDate,
                                      String weeklySummary, String dominantEmotion,
                                      ActionType recommendedAction, String recommendationMessage,
                                      String searchKeyword) {
        WeeklyReport report = new WeeklyReport();
        report.member = member;
        report.startDate = startDate;
        report.endDate = endDate;
        report.weeklySummary = weeklySummary;
        report.dominantEmotion = dominantEmotion;
        report.recommendedAction = recommendedAction;
        report.recommendationMessage = recommendationMessage;
        report.searchKeyword = searchKeyword;
        return report;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
