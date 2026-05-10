package hwan.project2.web.dto.community;

import jakarta.validation.constraints.NotBlank;

public record CommentCreateRequest(
        @NotBlank String content,
        Boolean isHidden
) {}
