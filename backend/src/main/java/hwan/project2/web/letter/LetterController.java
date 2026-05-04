package hwan.project2.web.letter;

import hwan.project2.security.UserPrincipal;
import hwan.project2.service.letter.LetterService;
import hwan.project2.web.dto.letter.LetterListItemResponse;
import hwan.project2.web.dto.letter.LetterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/letters")
@RequiredArgsConstructor
public class LetterController {

    private final LetterService letterService;

    @GetMapping
    public List<LetterListItemResponse> list(
            @AuthenticationPrincipal UserPrincipal principal) {
        return letterService.getAvailableLetters(principal.getId());
    }

    @GetMapping("/unread-count")
    public long unreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        return letterService.getUnreadCount(principal.getId());
    }

    @GetMapping("/{letterId}")
    public LetterResponse read(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long letterId) {
        return letterService.readLetter(principal.getId(), letterId);
    }
}
