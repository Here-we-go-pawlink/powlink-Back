package hwan.project2.domain.post;

import hwan.project2.domain.base.BaseTimeEntity;
import hwan.project2.domain.member.Member;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(indexes = {
        @Index(name = "idx_post_type_status", columnList = "type,status"),
        @Index(name = "idx_post_owner", columnList = "owner_id"),
        @Index(name = "idx_post_lat_lng", columnList = "lat,lng")
})
public class Post extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private Member owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(nullable = false, length = 300)
    private String summary;

    @Lob
    @Column(nullable = false)
    private String content;

    // 지도용 위치
    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lng;

    // 주소 문자열(선택): 화면 표시용
    @Column(length = 200)
    private String address;

    // 대표 이미지(선택): 목록/추천 카드용
    @Column(length = 500)
    private String thumbnailUrl;

    // null이면 정상, 값이 있으면 이 post는 다른 post로 병합됨
    @Column
    private Long mergedIntoPostId; // null이면 정상, 값 있으면 이 post는 다른 post로 병합됨


    public static Post create(Member owner, PostType type, String title, String summary, String content,
                              double lat, double lng, String address, String thumbnailUrl) {
        Post p = new Post();
        p.owner = owner;
        p.type = type;
        p.status = PostStatus.INVESTIGATING;
        p.title = title;
        p.summary = summary;
        p.content = content;
        p.lat = lat;
        p.lng = lng;
        p.address = address;
        p.thumbnailUrl = thumbnailUrl;
        return p;
    }

    public void resolve() { this.status = PostStatus.RESOLVED; }
    public void hide() { this.status = PostStatus.HIDDEN; }
}
