package hwan.project2.domain.letter;

import hwan.project2.domain.diary.Diary;
import hwan.project2.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "letters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Letter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "letter_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id")
    private Diary diary; // WEEKLY_REPORT인 경우 null

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LetterType type;

    @Column(nullable = false)
    private boolean isRead = false;

    @Column(nullable = false)
    private LocalDateTime deliverAt; // 이 시각 이후에 사용자에게 공개

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public static Letter ofDiaryReply(Member member, Diary diary, String content, LocalDateTime deliverAt) {
        Letter letter = new Letter();
        letter.member = member;
        letter.diary = diary;
        letter.content = content;
        letter.type = LetterType.DIARY_REPLY;
        letter.deliverAt = deliverAt;
        return letter;
    }

    public static Letter ofWeeklyReport(Member member, String content, LocalDateTime deliverAt) {
        Letter letter = new Letter();
        letter.member = member;
        letter.content = content;
        letter.type = LetterType.WEEKLY_REPORT;
        letter.deliverAt = deliverAt;
        return letter;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
