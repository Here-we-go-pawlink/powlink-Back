package hwan.project2.web.dto.letter;

import hwan.project2.domain.letter.Letter;
import hwan.project2.domain.letter.LetterType;

import java.time.LocalDateTime;

public record LetterListItemResponse(
        Long id,
        LetterType type,
        boolean isRead,
        LocalDateTime deliverAt,
        Long diaryId
) {
    public static LetterListItemResponse from(Letter letter) {
        return new LetterListItemResponse(
                letter.getId(),
                letter.getType(),
                letter.isRead(),
                letter.getDeliverAt(),
                letter.getDiary() != null ? letter.getDiary().getId() : null
        );
    }
}
