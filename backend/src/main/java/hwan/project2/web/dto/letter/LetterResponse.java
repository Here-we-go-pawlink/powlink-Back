package hwan.project2.web.dto.letter;

import hwan.project2.domain.letter.Letter;
import hwan.project2.domain.letter.LetterType;

import java.time.LocalDateTime;

public record LetterResponse(
        Long id,
        LetterType type,
        String content,
        boolean isRead,
        LocalDateTime deliverAt,
        Long diaryId
) {
    public static LetterResponse from(Letter letter) {
        return new LetterResponse(
                letter.getId(),
                letter.getType(),
                letter.getContent(),
                letter.isRead(),
                letter.getDeliverAt(),
                letter.getDiary() != null ? letter.getDiary().getId() : null
        );
    }
}
