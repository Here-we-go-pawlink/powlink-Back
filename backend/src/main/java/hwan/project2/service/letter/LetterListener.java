package hwan.project2.service.letter;

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
public class LetterListener {

    private final LetterService letterService;

    @Async("analysisExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnalysisCompleted(DiaryAnalysisCompletedEvent event) {
        log.info("편지 생성 시작: diaryId={}", event.diaryId());
        letterService.createDiaryReplyLetter(event.diaryId(), event.memberId());
    }
}
