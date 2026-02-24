package hwan.project2.domain.post;

import hwan.project2.domain.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(indexes = @Index(name = "idx_post_image_post", columnList = "post_id"))
public class PostImage extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private int sortOrder;

    public static PostImage of(Post post, String imageUrl, int sortOrder) {
        PostImage pi = new PostImage();
        pi.post = post;
        pi.imageUrl = imageUrl;
        pi.sortOrder = sortOrder;
        return pi;
    }
}
