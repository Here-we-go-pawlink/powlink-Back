package hwan.project2.web.dto.diary;

import hwan.project2.domain.diary.Diary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DiaryListItemResponse(
        Long id,
        String title,
        LocalDate diaryDate,
        String weather,
        String templateType,
        boolean isSecret,
        String status,
        LocalDateTime createdAt,
        List<SimpleUserEmotion> userEmotions
) {
    public record SimpleUserEmotion(String emotion, int score) {}

    public static DiaryListItemResponse from(Diary diary) {
        return new DiaryListItemResponse(
                diary.getId(),
                diary.getTitle(),
                diary.getDiaryDate(),
                diary.getWeather() != null ? diary.getWeather().name() : null,
                diary.getTemplateType() != null ? diary.getTemplateType().name() : "PLAIN",
                diary.isSecret(),
                diary.getStatus().name(),
                diary.getCreatedAt(),
                diary.getUserEmotions().stream()
                        .map(e -> new SimpleUserEmotion(e.getEmotionName(), e.getScore()))
                        .toList()
        );
    }
}
