package hwan.project2.service.diary;

import hwan.project2.domain.diary.Diary;
import hwan.project2.domain.diary.repo.DiaryRepository;
import hwan.project2.domain.member.Member;
import hwan.project2.domain.member.repo.MemberRepository;
import hwan.project2.exception.auth.MemberNotFoundException;
import hwan.project2.exception.diary.DiaryNotFoundException;
import hwan.project2.service.ai.DiaryCreatedEvent;
import hwan.project2.web.dto.diary.DiaryCreateRequest;
import hwan.project2.web.dto.diary.DiaryListItemResponse;
import hwan.project2.web.dto.diary.DiaryResponse;
import hwan.project2.web.dto.diary.DiaryUpdateRequest;
import hwan.project2.service.stats.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final StatsService statsService;

    @Transactional
    public Long createDiary(Long memberId, DiaryCreateRequest req) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        Diary diary = Diary.create(member, req.title(), req.content(),
                req.diaryDate(), req.weather(), req.templateType(), req.isSecret());
        if (req.imageUrls() != null && !req.imageUrls().isEmpty()) {
            diary.addImages(req.imageUrls());
        }
        Long diaryId = diaryRepository.save(diary).getId();
        eventPublisher.publishEvent(new DiaryCreatedEvent(diaryId)); // 커밋 후 AI 분석 트리거
        statsService.evictCache(memberId);
        return diaryId;
    }

    public DiaryResponse getDiary(Long memberId, Long diaryId) {
        Diary diary = diaryRepository.findByIdWithEmotions(diaryId, memberId)
                .orElseThrow(DiaryNotFoundException::new);
        return DiaryResponse.from(diary);
    }

    public Page<DiaryListItemResponse> getMyDiaries(Long memberId, Pageable pageable) {
        List<Diary> diaries = diaryRepository.findByMemberIdOrderByDateDesc(
                memberId, pageable.getOffset(), pageable.getPageSize());
        long total = diaryRepository.countByMemberId(memberId);
        return new PageImpl<>(
                diaries.stream().map(DiaryListItemResponse::from).toList(),
                pageable,
                total
        );
    }

    @Transactional
    public void updateDiary(Long memberId, Long diaryId, DiaryUpdateRequest req) {
        Diary diary = diaryRepository.findByIdWithEmotions(diaryId, memberId)
                .orElseThrow(DiaryNotFoundException::new);
        diary.update(req.title(), req.content(), req.weather(), req.templateType(), req.isSecret());
        diary.updateImages(req.imageUrls() != null ? req.imageUrls() : List.of());
        statsService.evictCache(memberId);
    }

    @Transactional
    public void deleteDiary(Long memberId, Long diaryId) {
        Diary diary = diaryRepository.findByIdWithEmotions(diaryId, memberId)
                .orElseThrow(DiaryNotFoundException::new);
        diaryRepository.delete(diary);
        statsService.evictCache(memberId);
    }
}
