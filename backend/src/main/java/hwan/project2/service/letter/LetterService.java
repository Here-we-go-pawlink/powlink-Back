package hwan.project2.service.letter;

import hwan.project2.domain.character.Character;
import hwan.project2.domain.character.repo.CharacterRepository;
import hwan.project2.domain.diary.Diary;
import hwan.project2.domain.diary.repo.DiaryRepository;
import hwan.project2.domain.letter.Letter;
import hwan.project2.domain.letter.repo.LetterRepository;
import hwan.project2.exception.letter.LetterNotFoundException;
import hwan.project2.web.dto.letter.LetterListItemResponse;
import hwan.project2.web.dto.letter.LetterResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LetterService {

    private final LetterRepository letterRepository;
    private final DiaryRepository diaryRepository;
    private final CharacterRepository characterRepository;
    private final LetterGenerationService letterGenerationService;

    @Transactional(readOnly = true)
    public List<LetterListItemResponse> getAvailableLetters(Long memberId) {
        return letterRepository
                .findByMemberIdAndDeliverAtLessThanEqualOrderByDeliverAtDesc(memberId, LocalDateTime.now())
                .stream()
                .map(LetterListItemResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long memberId) {
        return letterRepository.countByMemberIdAndIsReadFalseAndDeliverAtLessThanEqual(memberId, LocalDateTime.now());
    }

    @Transactional
    public LetterResponse readLetter(Long memberId, Long letterId) {
        Letter letter = letterRepository.findByIdAndMemberId(letterId, memberId)
                .orElseThrow(LetterNotFoundException::new);

        if (letter.getDeliverAt().isAfter(LocalDateTime.now())) {
            throw new LetterNotFoundException();
        }

        letter.markAsRead();
        return LetterResponse.from(letter);
    }

    @Transactional
    public void createImmediateLetter(Long diaryId, Long memberId) {
        Diary diary = diaryRepository.findByIdWithEmotions(diaryId, memberId).orElse(null);
        if (diary == null) {
            log.warn("즉시 편지 생성 실패 - 일기를 찾을 수 없음: diaryId={}", diaryId);
            return;
        }
        Character character = characterRepository.findByMemberId(memberId).orElse(null);
        try {
            String content = letterGenerationService.generate(diary, character);
            Letter letter = Letter.ofDiaryReply(diary.getMember(), diary, content, LocalDateTime.now());
            letterRepository.save(letter);
            log.info("즉시 편지 생성 완료: diaryId={}", diaryId);
        } catch (Exception e) {
            log.error("즉시 편지 생성 중 오류 발생: diaryId={}", diaryId, e);
        }
    }

    @Transactional
    public void createDiaryReplyLetter(Long diaryId, Long memberId) {
        Diary diary = diaryRepository.findByIdWithEmotions(diaryId, memberId).orElse(null);
        if (diary == null) {
            log.warn("편지 생성 실패 - 일기를 찾을 수 없음: diaryId={}", diaryId);
            return;
        }

        Character character = characterRepository.findByMemberId(memberId).orElse(null);

        try {
            String content = letterGenerationService.generate(diary, character);
            LocalDateTime deliverAt = LocalDate.now().plusDays(1).atTime(9, 0);
            Letter letter = Letter.ofDiaryReply(diary.getMember(), diary, content, deliverAt);
            letterRepository.save(letter);
            log.info("일기 답장 편지 생성 완료: diaryId={}, deliverAt={}", diaryId, deliverAt);
        } catch (Exception e) {
            log.error("편지 생성 중 오류 발생: diaryId={}", diaryId, e);
        }
    }
}
