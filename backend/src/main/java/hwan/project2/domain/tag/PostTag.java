package hwan.project2.domain.tag;

import hwan.project2.domain.base.BaseTimeEntity;
import hwan.project2.domain.post.Post;
import jakarta.persistence.*;
import lombok.*;


//N:N풀어주는 중간 엔티티
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(name = "uk_post_tag", columnNames = {"post_id", "tag_id"}),
        indexes = {
                @Index(name = "idx_posttag_post", columnList = "post_id"),
                @Index(name = "idx_posttag_tag", columnList = "tag_id")
        }
)
public class PostTag extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    public static PostTag of(Post post, Tag tag) {
        PostTag pt = new PostTag();
        pt.post = post;
        pt.tag = tag;
        return pt;
    }
}

