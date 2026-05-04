package hwan.project2.domain.diary;


import hwan.project2.domain.member.Member;
import hwan.project2.service.ai.dto.AnalysisResult;
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

    @Enumerated(EnumType.STRING)
    private TemplateType templateType;

    private boolean isSecret;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private AnalysisStatus status; // [PENDING, ANALYZING, COMPLETED, FAILED]

    @Column(columnDefinition = "TEXT")
    private String feedback; // AI 즉각 피드백 (one-liner)

    private LocalDateTime createdAt;

    // 감정 리스트 (1:N) — @BatchSize: 목록 조회 시 IN 쿼리로 일괄 로딩하여 N+1 방지
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiaryEmotion> emotions = new ArrayList<>();

    // 키워드 리스트 (1:N) — 주간 분석의 핵심 재료
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiaryKeyword> keywords = new ArrayList<>();

    // 추천 리스트 (1:N)
    @BatchSize(size = 10)
    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiaryRecommendation> recommendations = new ArrayList<>();

    // 이미지 리스트 (1:N)
    @BatchSize(size = 30)
    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("imageOrder ASC")
    private List<DiaryImage> images = new ArrayList<>();

    public static Diary create(Member member, String title, String content,
                               LocalDate diaryDate, Weather weather, TemplateType templateType, boolean isSecret) {
        Diary diary = new Diary();
        diary.member = member;
        diary.title = title;
        diary.content = content;
        diary.diaryDate = diaryDate;
        diary.weather = weather;
        diary.templateType = templateType != null ? templateType : TemplateType.PLAIN;
        diary.isSecret = isSecret;
        diary.status = AnalysisStatus.PENDING;
        return diary;
    }

    public void update(String title, String content, Weather weather, TemplateType templateType, boolean isSecret) {
        this.title = title;
        this.content = content;
        this.weather = weather;
        this.templateType = templateType != null ? templateType : TemplateType.PLAIN;
        this.isSecret = isSecret;
    }

    public void addImages(List<String> imageUrls) {
        for (int i = 0; i < imageUrls.size(); i++) {
            this.images.add(DiaryImage.of(this, imageUrls.get(i), i));
        }
    }

    public void updateImages(List<String> imageUrls) {
        this.images.clear();
        addImages(imageUrls);
    }

    public void startAnalyzing() {
        this.status = AnalysisStatus.ANALYZING;
    }

    public void completeAnalysis(AnalysisResult result) {
        // 재시도 시 이전 결과 제거 후 새로 저장 (중복 방지)
        this.emotions.clear();
        this.keywords.clear();
        this.recommendations.clear();
        result.emotions().forEach(e ->
                this.emotions.add(DiaryEmotion.create(this, e.name(), e.score())));
        result.keywords().forEach(w ->
                this.keywords.add(DiaryKeyword.create(this, w)));
        if (result.recommendations() != null) {
            result.recommendations().forEach(r ->
                    this.recommendations.add(DiaryRecommendation.create(this, r.type(), r.content())));
        }
        this.feedback = result.oneLiner();
        this.status = AnalysisStatus.COMPLETED;
    }

    public void failAnalysis() {
        this.status = AnalysisStatus.FAILED;
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
