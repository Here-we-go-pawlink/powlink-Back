package hwan.project2.domain.notification;

import hwan.project2.domain.base.BaseTimeEntity;
import hwan.project2.domain.member.Member;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(indexes = {
        @Index(name = "idx_noti_receiver_read_created", columnList = "receiver_id,readAt,createdAt"),
        @Index(name = "idx_noti_receiver_created", columnList = "receiver_id,createdAt")
})
public class Notification extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private Member receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    // 링크 타겟(예: postId, commentId, chatRoomId 등)
    private Long targetId;

    // 부가 메타(유사도 % 같은 값)
    @Column(length = 500)
    private String payloadJson;

    private java.time.LocalDateTime readAt;

    public static Notification of(Member receiver, NotificationType type, Long targetId, String payloadJson) {
        Notification n = new Notification();
        n.receiver = receiver;
        n.type = type;
        n.targetId = targetId;
        n.payloadJson = payloadJson;
        return n;
    }

    public boolean isRead() { return readAt != null; }

    public void markRead() {
        if (this.readAt == null) this.readAt = java.time.LocalDateTime.now();
    }
}
