package hwan.project2.domain.diary.repo;

import com.querydsl.jpa.impl.JPAQueryFactory;
import hwan.project2.domain.diary.AnalysisStatus;
import hwan.project2.domain.diary.Diary;
import hwan.project2.domain.diary.QDiary;
import hwan.project2.domain.diary.QDiaryEmotion;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class DiaryRepositoryImpl implements DiaryRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * emotions 을 JOIN FETCH 로 한 번에 가져와 N+1 제거.
     * keywords 는 @BatchSize(100) 덕분에 별도 1회 쿼리로 일괄 로딩.
     * member_id 조건을 함께 걸어 소유권 검증을 DB 레벨에서 수행.
     */
    @Override
    public Optional<Diary> findByIdWithEmotions(Long diaryId, Long memberId) {
        QDiary diary = QDiary.diary;
        QDiaryEmotion emotion = QDiaryEmotion.diaryEmotion;

        Diary result = queryFactory
                .selectFrom(diary)
                .leftJoin(diary.emotions, emotion).fetchJoin()
                .where(diary.id.eq(diaryId)
                        .and(diary.member.id.eq(memberId)))
                .fetchFirst();

        return Optional.ofNullable(result);
    }

    /**
     * 목록 조회는 메타데이터만 반환하므로 컬렉션 조인 없음 → N+1 미발생.
     */
    @Override
    public List<Diary> findByMemberIdOrderByDateDesc(Long memberId, long offset, int limit) {
        QDiary diary = QDiary.diary;

        return queryFactory
                .selectFrom(diary)
                .where(diary.member.id.eq(memberId))
                .orderBy(diary.diaryDate.desc(), diary.createdAt.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    @Override
    public long countByMemberId(Long memberId) {
        QDiary diary = QDiary.diary;

        Long count = queryFactory
                .select(diary.count())
                .from(diary)
                .where(diary.member.id.eq(memberId))
                .fetchOne();

        return count != null ? count : 0L;
    }

    /**
     * PENDING/FAILED 상태이고 생성된 지 5분 이상 지난 일기 조회.
     * ANALYZING 중인 건 건드리지 않음 (현재 처리 중일 수 있음).
     * 5분 버퍼: 방금 생성된 일기가 아직 분석 중인 경우를 제외하기 위함.
     */
    @Override
    public List<Diary> findUnanalyzed(int limit) {
        QDiary diary = QDiary.diary;
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);

        return queryFactory
                .selectFrom(diary)
                .where(diary.status.in(AnalysisStatus.PENDING, AnalysisStatus.FAILED)
                        .and(diary.createdAt.before(threshold)))
                .orderBy(diary.createdAt.asc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<Diary> findCompletedByMemberAndDateRange(Long memberId, LocalDate startDate, LocalDate endDate) {
        QDiary diary = QDiary.diary;
        QDiaryEmotion emotion = QDiaryEmotion.diaryEmotion;

        return queryFactory
                .selectDistinct(diary)
                .from(diary)
                .leftJoin(diary.emotions, emotion).fetchJoin()
                .where(diary.member.id.eq(memberId)
                        .and(diary.status.eq(AnalysisStatus.COMPLETED))
                        .and(diary.diaryDate.between(startDate, endDate)))
                .orderBy(diary.diaryDate.asc())
                .fetch();
    }

    @Override
    public List<Long> findMemberIdsWithEnoughDiaries(LocalDate startDate, LocalDate endDate, int minCount) {
        QDiary diary = QDiary.diary;

        return queryFactory
                .select(diary.member.id)
                .from(diary)
                .where(diary.status.eq(AnalysisStatus.COMPLETED)
                        .and(diary.diaryDate.between(startDate, endDate)))
                .groupBy(diary.member.id)
                .having(diary.count().goe(minCount))
                .fetch();
    }
}
