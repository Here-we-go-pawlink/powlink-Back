package hwan.project2.domain.community;

import hwan.project2.domain.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "community_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 50)
    private String emotionLabel;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @ElementCollection
    @CollectionTable(name = "community_post_tags", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "tag", length = 50)
    private List<String> tags = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public static CommunityPost create(Member member, String emotionLabel, String title, String content, List<String> tags) {
        CommunityPost post = new CommunityPost();
        post.member = member;
        post.emotionLabel = emotionLabel;
        post.title = title;
        post.content = content;
        if (tags != null) post.tags.addAll(tags);
        return post;
    }

    public void update(String emotionLabel, String title, String content, List<String> tags) {
        this.emotionLabel = emotionLabel;
        this.title = title;
        this.content = content;
        this.tags.clear();
        if (tags != null) this.tags.addAll(tags);
    }
}
