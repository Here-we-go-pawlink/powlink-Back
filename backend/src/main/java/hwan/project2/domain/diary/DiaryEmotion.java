package hwan.project2.domain.diary;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiaryEmotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id")
    private Diary diary;

    private String emotionName; // "우울", "기대" 등
    private int score;          // 0 ~ 100

    public static DiaryEmotion create(Diary diary, String emotionName, int score) {
        DiaryEmotion e = new DiaryEmotion();
        e.diary = diary;
        e.emotionName = emotionName;
        e.score = score;
        return e;
    }
}
