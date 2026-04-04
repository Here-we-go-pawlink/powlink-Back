package hwan.project2.web.dto.diary;

import hwan.project2.domain.diary.Diary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DiaryResponse(
        Long id,
        String title,
        String content,
        LocalDate diaryDate,
        String weather,
        boolean isSecret,
        String status,
        LocalDateTime createdAt,
        List<EmotionResponse> emotions,
        List<String> keywords,
        List<String> imageUrls
) {
    public static DiaryResponse from(Diary diary) {
        return new DiaryResponse(
                diary.getId(),
                diary.getTitle(),
                diary.getContent(),
                diary.getDiaryDate(),
                diary.getWeather() != null ? diary.getWeather().name() : null,
                diary.isSecret(),
                diary.getStatus().name(),
                diary.getCreatedAt(),
                diary.getEmotions().stream()
                        .map(e -> new EmotionResponse(e.getEmotionName(), e.getScore()))
                        .toList(),
                diary.getKeywords().stream()
                        .map(k -> k.getWord())
                        .toList(),
                diary.getImages().stream()
                        .map(img -> img.getImageUrl())
                        .toList()
        );
    }
}
