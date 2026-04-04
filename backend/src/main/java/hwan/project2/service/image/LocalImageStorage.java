package hwan.project2.service.image;

import hwan.project2.exception.image.ImageUploadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Component
public class LocalImageStorage implements ImageStorage {

    @Value("${image.upload-dir}")
    private String uploadDir;

    @Override
    public String upload(MultipartFile file, String directory) {
        String filename = UUID.randomUUID() + getExtension(file.getOriginalFilename());
        Path targetDir = Paths.get(uploadDir, directory);
        Path targetPath = targetDir.resolve(filename);

        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetPath);
        } catch (IOException e) {
            log.error("Failed to save image: {}", targetPath, e);
            throw new ImageUploadException("이미지 저장에 실패했습니다.");
        }

        return "/images/" + directory + "/" + filename;
    }

    @Override
    public void delete(String imageUrl) {
        // /images/diary/xxx.jpg → {uploadDir}/diary/xxx.jpg
        String relativePath = imageUrl.replaceFirst("^/images/", "");
        Path filePath = Paths.get(uploadDir, relativePath);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete image: {}", filePath, e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }
}
