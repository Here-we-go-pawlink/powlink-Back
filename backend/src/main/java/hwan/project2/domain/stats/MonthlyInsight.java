package hwan.project2.domain.stats;

import hwan.project2.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonthlyInsight {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "monthly_insight_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "insight_month", nullable = false, length = 7)
    private String yearMonth; // "2026-05"

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String trend;

    @Column(columnDefinition = "TEXT")
    private String recommendationsJson; // JSON array e.g. ["...", "...", "..."]

    private LocalDateTime createdAt;

    public static MonthlyInsight create(Member member, String yearMonth,
                                        String summary, String trend,
                                        String recommendationsJson) {
        MonthlyInsight insight = new MonthlyInsight();
        insight.member = member;
        insight.yearMonth = yearMonth;
        insight.summary = summary;
        insight.trend = trend;
        insight.recommendationsJson = recommendationsJson;
        return insight;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
