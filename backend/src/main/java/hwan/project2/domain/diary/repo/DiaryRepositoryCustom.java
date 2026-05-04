package hwan.project2.domain.diary.repo;

import hwan.project2.domain.diary.Diary;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DiaryRepositoryCustom {

    /** 단건 조회: emotions JOIN FETCH, 소유권 검증 포함 */
    Optional<Diary> findByIdWithEmotions(Long diaryId, Long memberId);

    /** 목록 조회: 메타데이터만 (컬렉션 미포함), 최신순 */
    List<Diary> findByMemberIdOrderByDateDesc(Long memberId, long offset, int limit);

    long countByMemberId(Long memberId);

    /** 분석 미완료 일기 조회 (PENDING/FAILED) - 재시도 스케줄러용 */
    List<Diary> findUnanalyzed(int limit);

    /** 주간 리포트용: 특정 기간 COMPLETED 일기 + 감정 fetch (키워드는 @BatchSize 로딩) */
    List<Diary> findCompletedByMemberAndDateRange(Long memberId, LocalDate startDate, LocalDate endDate);

    /** 주간 리포트 대상: 해당 기간 COMPLETED 일기 3편 이상인 멤버 ID 목록 */
    List<Long> findMemberIdsWithEnoughDiaries(LocalDate startDate, LocalDate endDate, int minCount);
}
