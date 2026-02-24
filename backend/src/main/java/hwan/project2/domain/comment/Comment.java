package hwan.project2.domain.comment;

import hwan.project2.domain.base.BaseTimeEntity;
import hwan.project2.domain.member.Member;
import hwan.project2.domain.post.Post;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(indexes = {
        @Index(name = "idx_comment_post_root_created", columnList = "post_id,root_id,createdAt")
})
public class Comment extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Member author;

    // 루트 댓글이면 null, 대댓글이면 부모 댓글 id
    private Long parentId;

    // ✅ 루트 댓글이면 null, 대댓글이면 "루트 댓글 id"
    private Long rootId;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private boolean deleted = false;

    public static Comment createRoot(Post post, Member author, String content) {
        Comment c = new Comment();
        c.post = post;
        c.author = author;
        c.content = content;
        c.parentId = null;
        c.rootId = null; // ✅ 루트는 null 고정 (UPDATE 없음)
        return c;
    }

    /**
     * @param parentId 대댓글의 부모 댓글 id (2단계면 보통 루트 댓글 id)
     * @param rootId   루트 댓글 id (반드시 루트 댓글의 id)
     */
    public static Comment createReply(Post post, Member author, Long parentId, Long rootId, String content) {
        Comment c = new Comment();
        c.post = post;
        c.author = author;
        c.content = content;
        c.parentId = parentId;
        c.rootId = rootId;
        return c;
    }

    public void softDelete() {
        this.deleted = true;
        this.content = "[deleted]";
    }
}
