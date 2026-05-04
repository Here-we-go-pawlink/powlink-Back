package hwan.project2.web.dto.diary;

import hwan.project2.domain.diary.Diary;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DiaryListItemResponse(
        Long id,
        String title,
        LocalDate diaryDate,
        String weather,
        String templateType,
        boolean isSecret,
        String status,
        LocalDateTime createdAt
) {
    public static DiaryListItemResponse from(Diary diary) {
        return new DiaryListItemResponse(
                diary.getId(),
                diary.getTitle(),
                diary.getDiaryDate(),
                diary.getWeather() != null ? diary.getWeather().name() : null,
                diary.getTemplateType() != null ? diary.getTemplateType().name() : "PLAIN",
                diary.isSecret(),
                diary.getStatus().name(),
                diary.getCreatedAt()
        );
    }
}
