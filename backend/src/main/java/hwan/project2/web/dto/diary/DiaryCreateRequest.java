package hwan.project2.web.dto.diary;

import hwan.project2.domain.diary.TemplateType;
import hwan.project2.domain.diary.Weather;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record DiaryCreateRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank String content,
        @NotNull LocalDate diaryDate,
        Weather weather,
        TemplateType templateType,
        boolean isSecret,
        List<String> imageUrls
) {}
