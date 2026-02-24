package hwan.project2.domain.chat;

import hwan.project2.domain.base.BaseTimeEntity;
import hwan.project2.domain.member.Member;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(indexes = {
        @Index(name = "idx_message_room_id", columnList = "room_id,id")
})
public class ChatMessage extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    // sender는 클라가 주는 값 믿으면 안 됨 (서버에서 Principal로 확정)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private Member sender;

    @Lob
    @Column(nullable = false)
    private String content;

    public static ChatMessage of(ChatRoom room, Member sender, String content) {
        ChatMessage m = new ChatMessage();
        m.room = room;
        m.sender = sender;
        m.content = content;
        return m;
    }
}
