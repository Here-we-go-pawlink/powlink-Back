package hwan.project2.domain.diary;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiaryKeyword {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id")
    private Diary diary;

    private String word; // "진로", "에러", "면접" 등

    public static DiaryKeyword create(Diary diary, String word) {
        DiaryKeyword k = new DiaryKeyword();
        k.diary = diary;
        k.word = word;
        return k;
    }
}
