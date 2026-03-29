package hwan.project2.domain.diary;


import hwan.project2.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Diary {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "diary_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private String title;
    private LocalDate diaryDate;

    @Enumerated(EnumType.STRING)
    private Weather weather;

    private boolean isSecret;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private AnalysisStatus status; // [PENDING, ANALYZING, COMPLETED]

    private LocalDateTime createdAt;

    // 감정 리스트 (1:N) — @BatchSize: 목록 조회 시 IN 쿼리로 일괄 로딩하여 N+1 방지
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiaryEmotion> emotions = new ArrayList<>();

    // 키워드 리스트 (1:N) — 주간 분석의 핵심 재료
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiaryKeyword> keywords = new ArrayList<>();

    public static Diary create(Member member, String title, String content,
                               LocalDate diaryDate, Weather weather, boolean isSecret) {
        Diary diary = new Diary();
        diary.member = member;
        diary.title = title;
        diary.content = content;
        diary.diaryDate = diaryDate;
        diary.weather = weather;
        diary.isSecret = isSecret;
        diary.status = AnalysisStatus.PENDING;
        return diary;
    }

    public void update(String title, String content, Weather weather, boolean isSecret) {
        this.title = title;
        this.content = content;
        this.weather = weather;
        this.isSecret = isSecret;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
