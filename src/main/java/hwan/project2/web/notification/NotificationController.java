package hwan.project2.web.notification;

import hwan.project2.security.UserPrincipal;
import hwan.project2.security.jwt.JwtTokenProvider;
import hwan.project2.service.notification.NotificationService;
import hwan.project2.service.notification.SseEmitterService;
import hwan.project2.web.dto.notification.NotificationCountResponse;
import hwan.project2.web.dto.notification.NotificationResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterService sseEmitterService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.getNotifications(principal.getId()));
    }

    @GetMapping("/count")
    public ResponseEntity<NotificationCountResponse> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.getUnreadCount(principal.getId()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        notificationService.markAsRead(principal.getId(), id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * SSE 스트림. EventSource는 Authorization 헤더를 지원하지 않으므로
     * 액세스 토큰을 쿼리 파라미터로 수신하여 직접 검증한다.
     * SecurityConfig에서 이 경로를 permitAll()로 설정해야 한다.
     *
     * 인증 실패 시 예외를 던지지 않고 즉시 완료되는 emitter를 반환한다.
     * (SSE 엔드포인트에서 예외를 던지면 GlobalExceptionHandler가 JSON을 쓰려다
     *  text/event-stream 과 충돌하여 HttpMediaTypeNotAcceptableException 발생)
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam(required = false) String token) {
        Long memberId = resolveMemberId(token);
        if (memberId == null) {
            // 인증 실패 → AUTH_ERROR 이벤트 전송 후 즉시 종료
            return closedEmitterWithError();
        }
        return sseEmitterService.subscribe(memberId);
    }

    private Long resolveMemberId(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            Claims claims = jwtTokenProvider.parseAndValidate(token);
            Long memberId = jwtTokenProvider.getMemberId(claims);
            if (memberId == null) {
                log.warn("SSE 연결 거부 - 토큰에 memberId 없음");
            }
            return memberId;
        } catch (JwtException e) {
            log.warn("SSE 연결 거부 - 유효하지 않은 토큰: {}", e.getMessage());
            return null;
        }
    }

    private SseEmitter closedEmitterWithError() {
        SseEmitter emitter = new SseEmitter(0L);
        try {
            emitter.send(SseEmitter.event().name("AUTH_ERROR").data("invalid or expired token"));
        } catch (IOException ignored) {}
        emitter.complete();
        return emitter;
    }
}
