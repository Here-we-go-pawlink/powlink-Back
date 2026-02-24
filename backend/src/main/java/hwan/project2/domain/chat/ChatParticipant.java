package hwan.project2.domain.chat;

import hwan.project2.domain.base.BaseTimeEntity;
import hwan.project2.domain.member.Member;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(name = "uk_room_member", columnNames = {"room_id", "member_id"}),
        indexes = {
                @Index(name = "idx_participant_member", columnList = "member_id"),
                @Index(name = "idx_participant_room", columnList = "room_id")
        }
)
public class ChatParticipant extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 읽음 기준: 마지막으로 읽은 메시지 id
    private Long lastReadMessageId;

    public static ChatParticipant of(ChatRoom room, Member member) {
        ChatParticipant p = new ChatParticipant();
        p.room = room;
        p.member = member;
        p.lastReadMessageId = 0L;
        return p;
    }

    public void readUpTo(Long messageId) {
        this.lastReadMessageId = messageId;
    }
}
