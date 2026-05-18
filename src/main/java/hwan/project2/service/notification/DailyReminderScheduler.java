package hwan.project2.service.notification;

import hwan.project2.domain.diary.repo.DiaryRepository;
import hwan.project2.domain.letter.Letter;
import hwan.project2.domain.letter.repo.LetterRepository;
import hwan.project2.domain.member.Member;
import hwan.project2.domain.member.MemberStatus;
import hwan.project2.domain.member.repo.MemberRepository;
import hwan.project2.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyReminderScheduler {

    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final DiaryRepository diaryRepository;
    private final LetterRepository letterRepository;

    // 매일 저녁 8시 실행
    @Scheduled(cron = "0 0 20 * * *")
    @Transactional
    public void sendDailyReminders() {
        List<Member> targets = memberRepository.findByStatusAndNotifyDailyReminderTrue(MemberStatus.ACTIVE);
        LocalDate today = LocalDate.now();
        log.info("일기 작성 알림 대상 후보: {}명", targets.size());

        int sent = 0;
        for (Member member : targets) {
            if (diaryRepository.existsByMemberIdAndDiaryDate(member.getId(), today)) {
                continue; // 오늘 이미 일기를 쓴 유저는 알림 제외
            }
            notificationService.create(
                    member.getId(),
                    NotificationType.DAILY_REMINDER,
                    "오늘 일기를 작성해보세요",
                    "오늘 하루는 어떠셨나요? 감정을 기록해보세요",
                    "/write"
            );
            sent++;
        }
        log.info("일기 작성 알림 전송 완료: {}명 ({}명 제외 - 이미 작성)", sent, targets.size() - sent);
    }

    // 매일 오전 9시: 오늘 배달될 편지 알림 발송
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void sendLetterDeliveryNotifications() {
        LocalDateTime start = LocalDate.now().atTime(9, 0);
        LocalDateTime end = start.plusMinutes(10); // 스케줄러 지연 여유
        sendLetterNotifications(letterRepository.findByDeliverAtBetween(start, end));
    }

    /** dev/테스트 전용: 현재 사용자의 미배달 편지를 즉시 배달 처리하고 알림 발송 */
    @Transactional
    public int deliverPendingLettersForMember(Long memberId) {
        List<Letter> pending = letterRepository.findByMemberIdAndDeliverAtAfter(memberId, LocalDateTime.now());
        pending.forEach(Letter::deliverNow);
        sendLetterNotifications(pending);
        return pending.size();
    }

    private void sendLetterNotifications(List<Letter> letters) {
        for (Letter letter : letters) {
            Member member = letter.getMember();
            if (member == null) continue;
            notificationService.create(
                    member.getId(),
                    NotificationType.LETTER,
                    "편지가 도착했어요",
                    "AI 친구가 따뜻한 편지를 보냈어요",
                    "/letters"
            );
        }
        log.info("편지 도착 알림 발송: {}건", letters.size());
    }
}
