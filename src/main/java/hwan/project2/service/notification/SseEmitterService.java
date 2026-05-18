package hwan.project2.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import hwan.project2.web.dto.notification.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseEmitterService {

    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L; // 30분

    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SseEmitter subscribe(Long memberId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emitters.compute(memberId, (k, old) -> {
            if (old != null) old.complete();
            return emitter;
        });

        emitter.onCompletion(() -> emitters.remove(memberId, emitter));
        emitter.onTimeout(() -> emitters.remove(memberId, emitter));
        emitter.onError(e -> emitters.remove(memberId, emitter));

        // 연결 확인 이벤트 (EventSource가 처음 열릴 때 필요)
        try {
            emitter.send(SseEmitter.event().name("CONNECTED").data("connected"));
        } catch (IOException e) {
            emitters.remove(memberId, emitter);
            emitter.completeWithError(e);
        }

        log.debug("SSE 연결: memberId={}, 현재 연결 수={}", memberId, emitters.size());
        return emitter;
    }

    public void send(Long memberId, NotificationResponse dto) {
        SseEmitter emitter = emitters.get(memberId);
        if (emitter == null) return;

        try {
            String json = objectMapper.writeValueAsString(dto);
            emitter.send(SseEmitter.event().name("NOTIFICATION").data(json, MediaType.APPLICATION_JSON));
            log.debug("SSE 전송 완료: memberId={}, type={}", memberId, dto.type());
        } catch (IOException e) {
            log.debug("SSE 전송 실패 (연결 종료): memberId={}", memberId);
            emitters.remove(memberId, emitter);
        } catch (Exception e) {
            log.warn("SSE 전송 오류: memberId={}", memberId, e);
        }
    }
}
