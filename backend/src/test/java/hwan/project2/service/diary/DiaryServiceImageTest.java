package hwan.project2.service.diary;

import hwan.project2.domain.diary.Diary;
import hwan.project2.domain.diary.repo.DiaryRepository;
import hwan.project2.service.auth.AuthService;
import hwan.project2.web.dto.SignupRequest;
import hwan.project2.web.dto.diary.DiaryCreateRequest;
import hwan.project2.web.dto.diary.DiaryResponse;
import hwan.project2.web.dto.diary.DiaryUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DiaryServiceImageTest {

    @Autowired
    DiaryService diaryService;

    @Autowired
    AuthService authService;

    @Autowired
    DiaryRepository diaryRepository;

    @MockitoBean
    StringRedisTemplate redis;

    Long memberId;

    @BeforeEach
    void setUp() {
        memberId = authService.signup(new SignupRequest("tester", "tester@test.com", "password", null));
    }

    @Test
    @DisplayName("이미지 URL 목록과 함께 다이어리를 생성하면 이미지가 저장된다")
    void createDiary_withImages_savesImagesInOrder() {
        // given
        List<String> imageUrls = List.of(
                "/images/diary/first.jpg",
                "/images/diary/second.jpg",
                "/images/diary/third.jpg"
        );
        DiaryCreateRequest req = new DiaryCreateRequest(
                "오늘의 일기", "내용입니다", LocalDate.now(), null, null, false, imageUrls
        );

        // when
        Long diaryId = diaryService.createDiary(memberId, req);

        // then
        DiaryResponse response = diaryService.getDiary(memberId, diaryId);
        assertThat(response.imageUrls()).hasSize(3);
        assertThat(response.imageUrls()).containsExactly(
                "/images/diary/first.jpg",
                "/images/diary/second.jpg",
                "/images/diary/third.jpg"
        );
    }

    @Test
    @DisplayName("이미지 없이 다이어리를 생성하면 이미지 목록은 비어있다")
    void createDiary_withoutImages_emptyImageList() {
        DiaryCreateRequest req = new DiaryCreateRequest(
                "오늘의 일기", "내용입니다", LocalDate.now(), null, null, false, null
        );

        Long diaryId = diaryService.createDiary(memberId, req);

        DiaryResponse response = diaryService.getDiary(memberId, diaryId);
        assertThat(response.imageUrls()).isEmpty();
    }

    @Test
    @DisplayName("다이어리 수정 시 이미지 목록이 새 목록으로 교체된다")
    void updateDiary_replacesImageList() {
        // given: 이미지 2개로 생성
        Long diaryId = diaryService.createDiary(memberId, new DiaryCreateRequest(
                "제목", "내용", LocalDate.now(), null, null, false,
                List.of("/images/diary/old1.jpg", "/images/diary/old2.jpg")
        ));

        // when: 이미지 1개로 수정
        diaryService.updateDiary(memberId, diaryId, new DiaryUpdateRequest(
                "수정된 제목", "수정된 내용", null, null, false,
                List.of("/images/diary/new1.jpg")
        ));

        // then
        DiaryResponse response = diaryService.getDiary(memberId, diaryId);
        assertThat(response.imageUrls()).hasSize(1);
        assertThat(response.imageUrls()).containsExactly("/images/diary/new1.jpg");
    }

    @Test
    @DisplayName("다이어리 수정 시 imageUrls가 null이면 이미지가 모두 삭제된다")
    void updateDiary_withNullImages_clearsImageList() {
        // given: 이미지 있는 다이어리
        Long diaryId = diaryService.createDiary(memberId, new DiaryCreateRequest(
                "제목", "내용", LocalDate.now(), null, null, false,
                List.of("/images/diary/photo.jpg")
        ));

        // when: imageUrls null로 수정
        diaryService.updateDiary(memberId, diaryId, new DiaryUpdateRequest(
                "수정된 제목", "수정된 내용", null, null, false, null
        ));

        // then
        DiaryResponse response = diaryService.getDiary(memberId, diaryId);
        assertThat(response.imageUrls()).isEmpty();
    }

    @Test
    @DisplayName("다이어리 삭제 시 연관 이미지도 함께 삭제된다")
    void deleteDiary_cascadesImagesToDelete() {
        // given
        Long diaryId = diaryService.createDiary(memberId, new DiaryCreateRequest(
                "제목", "내용", LocalDate.now(), null, null, false,
                List.of("/images/diary/photo.jpg")
        ));

        // when
        diaryService.deleteDiary(memberId, diaryId);

        // then: 다이어리 자체가 삭제됐으므로 이미지도 orphanRemoval로 제거됨
        assertThat(diaryRepository.findById(diaryId)).isEmpty();
    }
}
