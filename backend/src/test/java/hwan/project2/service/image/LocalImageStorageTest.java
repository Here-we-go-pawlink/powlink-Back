package hwan.project2.service.image;

import hwan.project2.exception.image.ImageUploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalImageStorageTest {

    @TempDir
    Path tempDir;

    LocalImageStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalImageStorage();
        ReflectionTestUtils.setField(storage, "uploadDir", tempDir.toString());
    }

    @Test
    @DisplayName("파일 업로드 시 디렉토리가 자동으로 생성되고 파일이 저장된다")
    void upload_createsDirectoryAndSavesFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "image-data".getBytes()
        );

        String url = storage.upload(file, "diary");

        assertThat(url).startsWith("/images/diary/");
        assertThat(url).endsWith(".jpg");

        // 실제 파일이 저장됐는지 확인
        String filename = url.replace("/images/diary/", "");
        Path savedFile = tempDir.resolve("diary").resolve(filename);
        assertThat(Files.exists(savedFile)).isTrue();
        assertThat(Files.readAllBytes(savedFile)).isEqualTo("image-data".getBytes());
    }

    @Test
    @DisplayName("확장자가 없는 파일도 업로드된다")
    void upload_fileWithoutExtension_succeeds() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo", "image/jpeg", "image-data".getBytes()
        );

        String url = storage.upload(file, "profile");

        assertThat(url).startsWith("/images/profile/");
        assertThat(url).doesNotContain(".");
    }

    @Test
    @DisplayName("파일 삭제 시 실제 파일이 제거된다")
    void delete_removesFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", "image-data".getBytes()
        );
        String url = storage.upload(file, "diary");

        storage.delete(url);

        String filename = url.replace("/images/diary/", "");
        Path deletedFile = tempDir.resolve("diary").resolve(filename);
        assertThat(Files.exists(deletedFile)).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 파일을 삭제해도 예외가 발생하지 않는다")
    void delete_nonExistentFile_noException() {
        // 예외 없이 정상 종료되어야 함
        storage.delete("/images/diary/nonexistent.jpg");
    }

    @Test
    @DisplayName("저장 실패 시 ImageUploadException이 발생한다")
    void upload_ioFailure_throwsImageUploadException() throws IOException {
        // 디렉토리 경로에 미리 파일을 만들어두면 createDirectories가 실패함
        Path blockedDir = tempDir.resolve("blocked");
        Files.createFile(blockedDir); // 파일로 만들어서 하위 디렉토리 생성 불가

        ReflectionTestUtils.setField(storage, "uploadDir", tempDir.toString());

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "data".getBytes()
        );

        assertThatThrownBy(() -> storage.upload(file, "blocked"))
                .isInstanceOf(ImageUploadException.class)
                .hasMessageContaining("이미지 저장에 실패");
    }
}
