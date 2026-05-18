package hwan.project2.service.notification;

import hwan.project2.domain.member.Member;
import hwan.project2.domain.member.repo.MemberRepository;
import hwan.project2.domain.notification.NotificationType;
import hwan.project2.service.ai.DiaryAnalysisCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationService notificationService;
    private final MemberRepository memberRepository;

    @Async("analysisExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnalysisCompleted(DiaryAnalysisCompletedEvent event) {
        Member member = memberRepository.findById(event.memberId()).orElse(null);
        if (member == null || !member.isNotifyAiAnalysis()) return;

        notificationService.create(
                event.memberId(),
                NotificationType.ANALYSIS,
                "일기 분석이 완료됐어요",
                "오늘 작성한 일기의 감정 분석 결과를 확인해보세요",
                "/diary/" + event.diaryId()
        );
    }

    @Async("analysisExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWeeklyReportCreated(WeeklyReportCreatedEvent event) {
        Member member = memberRepository.findById(event.memberId()).orElse(null);
        if (member == null || !member.isNotifyWeeklyReport()) return;

        notificationService.create(
                event.memberId(),
                NotificationType.WEEKLY,
                "주간 리포트가 생성됐어요",
                "이번 주 감정 흐름을 AI가 분석했어요",
                "/weekly-report"
        );
    }
}
