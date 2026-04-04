package hwan.project2.service.image;

import hwan.project2.exception.image.ImageUploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    ImageStorage imageStorage;

    ImageService imageService;

    @BeforeEach
    void setUp() {
        imageService = new ImageService(imageStorage);
    }

    @Test
    @DisplayName("유효한 이미지 파일은 업로드에 성공하고 URL을 반환한다")
    void upload_validFile_returnsUrl() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[1024]
        );
        given(imageStorage.upload(file, "diary")).willReturn("/images/diary/uuid.jpg");

        // when
        String url = imageService.upload(file, "diary");

        // then
        assertThat(url).isEqualTo("/images/diary/uuid.jpg");
        then(imageStorage).should().upload(file, "diary");
    }

    @Test
    @DisplayName("빈 파일은 업로드 시 예외가 발생한다")
    void upload_emptyFile_throwsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[0]
        );

        assertThatThrownBy(() -> imageService.upload(emptyFile, "diary"))
                .isInstanceOf(ImageUploadException.class)
                .hasMessageContaining("비어있습니다");
    }

    @Test
    @DisplayName("5MB 초과 파일은 업로드 시 예외가 발생한다")
    void upload_tooLargeFile_throwsException() {
        MockMultipartFile bigFile = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", new byte[6 * 1024 * 1024]
        );

        assertThatThrownBy(() -> imageService.upload(bigFile, "diary"))
                .isInstanceOf(ImageUploadException.class)
                .hasMessageContaining("5MB");
    }

    @Test
    @DisplayName("허용되지 않는 파일 형식은 업로드 시 예외가 발생한다")
    void upload_invalidContentType_throwsException() {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", new byte[1024]
        );

        assertThatThrownBy(() -> imageService.upload(pdfFile, "diary"))
                .isInstanceOf(ImageUploadException.class)
                .hasMessageContaining("지원하지 않는");
    }

    @Test
    @DisplayName("delete는 ImageStorage에 위임한다")
    void delete_delegatesToStorage() {
        imageService.delete("/images/diary/uuid.jpg");

        then(imageStorage).should().delete("/images/diary/uuid.jpg");
    }
}
