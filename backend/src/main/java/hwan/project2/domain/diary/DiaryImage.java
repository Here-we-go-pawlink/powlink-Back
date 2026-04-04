package hwan.project2.domain.diary;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiaryImage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "diary_image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_id")
    private Diary diary;

    private String imageUrl;
    private int imageOrder;

    public static DiaryImage of(Diary diary, String imageUrl, int order) {
        DiaryImage image = new DiaryImage();
        image.diary = diary;
        image.imageUrl = imageUrl;
        image.imageOrder = order;
        return image;
    }
}
