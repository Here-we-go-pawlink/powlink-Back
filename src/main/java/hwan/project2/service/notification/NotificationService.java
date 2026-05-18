package hwan.project2.service.notification;

import hwan.project2.domain.member.Member;
import hwan.project2.domain.member.repo.MemberRepository;
import hwan.project2.domain.notification.Notification;
import hwan.project2.domain.notification.NotificationType;
import hwan.project2.domain.notification.repo.NotificationRepository;
import hwan.project2.web.dto.notification.NotificationCountResponse;
import hwan.project2.web.dto.notification.NotificationResponse;
import hwan.project2.web.dto.notification.NotificationSettingsResponse;
import hwan.project2.web.dto.notification.NotificationSettingsUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final SseEmitterService sseEmitterService;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long memberId) {
        return notificationRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream().map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public NotificationCountResponse getUnreadCount(Long memberId) {
        return new NotificationCountResponse(
                notificationRepository.countByMemberIdAndIsReadFalse(memberId));
    }

    public void markAsRead(Long memberId, Long notificationId) {
        notificationRepository.findByIdAndMemberId(notificationId, memberId)
                .ifPresent(Notification::markAsRead);
    }

    public void markAllAsRead(Long memberId) {
        notificationRepository.markAllAsReadByMemberId(memberId);
    }

    @Transactional(readOnly = true)
    public NotificationSettingsResponse getSettings(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        return NotificationSettingsResponse.from(member);
    }

    public void updateSettings(Long memberId, NotificationSettingsUpdateRequest req) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        member.updateNotificationSettings(
                req.notify_daily_reminder(),
                req.notify_ai_analysis(),
                req.notify_weekly_report(),
                req.notify_friend_activity()
        );
    }

    /** 알림 생성 후 SSE 푸시. 리스너/스케줄러에서 호출. */
    public void create(Long memberId, NotificationType type, String title, String body, String link) {
        // getReferenceById: FK 저장만 하므로 실제 SELECT 불필요
        Member member = memberRepository.getReferenceById(memberId);
        Notification saved = notificationRepository.save(Notification.of(member, type, title, body, link));
        log.info("알림 생성: memberId={}, type={}", memberId, type);

        sseEmitterService.send(memberId, NotificationResponse.from(saved));
    }
}
