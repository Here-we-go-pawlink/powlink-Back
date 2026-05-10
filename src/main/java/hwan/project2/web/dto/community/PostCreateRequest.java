package hwan.project2.web.dto.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostCreateRequest(
        @NotBlank String emotionLabel,
        @NotBlank @Size(max = 100) String title,
        @NotBlank String content,
        @Size(max = 5) List<String> tags
) {}
