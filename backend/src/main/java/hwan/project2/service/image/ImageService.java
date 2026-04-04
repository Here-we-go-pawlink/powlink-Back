package hwan.project2.service.image;

import hwan.project2.exception.image.ImageUploadException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final ImageStorage imageStorage;

    public String upload(MultipartFile file, String directory) {
        validate(file);
        return imageStorage.upload(file, directory);
    }

    public void delete(String imageUrl) {
        imageStorage.delete(imageUrl);
    }

    private void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ImageUploadException("파일이 비어있습니다.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ImageUploadException("파일 크기는 5MB를 초과할 수 없습니다.");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new ImageUploadException("지원하지 않는 이미지 형식입니다. (jpg, png, webp, gif)");
        }
    }
}
