package hwan.project2.domain.chat;

import hwan.project2.domain.base.BaseTimeEntity;
import hwan.project2.domain.post.Post;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(indexes = {
        @Index(name = "idx_chat_room_post", columnList = "post_id")
})
public class ChatRoom extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // 목록 성능용(마지막 메시지 요약)
    @Column(length = 300)
    private String lastMessage;

    private Long lastMessageAt;

    public static ChatRoom of(Post post) {
        ChatRoom r = new ChatRoom();
        r.post = post;
        return r;
    }

    public void updateLastMessage(String lastMessage, long epochMillis) {
        this.lastMessage = lastMessage;
        this.lastMessageAt = epochMillis;
    }
}
