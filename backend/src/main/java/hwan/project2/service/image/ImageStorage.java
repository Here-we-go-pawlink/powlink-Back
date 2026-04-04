package hwan.project2.service.image;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorage {
    String upload(MultipartFile file, String directory);
    void delete(String imageUrl);
}
