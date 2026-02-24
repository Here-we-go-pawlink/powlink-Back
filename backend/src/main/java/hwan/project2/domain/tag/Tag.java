package hwan.project2.domain.tag;

import hwan.project2.domain.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(name = "uk_tag_slug", columnNames = {"slug"}),
        indexes = {
                @Index(name = "idx_tag_name", columnList = "name")
        }
)
public class Tag extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 화면 표시용 (예: "푸들", "흰색", "목줄")
    @Column(nullable = false, length = 50)
    private String name;

    // 검색/중복 방지용 정규화 키 (예: "poodle", "white", "collar")
    @Column(nullable = false, length = 80)
    private String slug;

    public static Tag of(String name, String slug) {
        Tag t = new Tag();
        t.name = name;
        t.slug = slug;
        return t;
    }
}
