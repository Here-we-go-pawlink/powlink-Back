package hwan.project2.web.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import hwan.project2.security.UserPrincipal;

@RestController
@RequestMapping("/api/test")
public class SecurityTestController {

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    @GetMapping("/secured")
    public ResponseEntity<String> secured(@AuthenticationPrincipal UserPrincipal principal) {
        // authenticated()면 principal null로 안 들어오는게 정상
        return ResponseEntity.ok("ok:" + principal.getId());
    }

    @GetMapping("/admin-only")
    public ResponseEntity<String> adminOnly() {
        // SecurityConfig의 /api/admin/** 를 쓰고 싶으면 아래처럼 경로를 /api/admin/test로 바꿔도 됨
        return ResponseEntity.ok("admin ok");
    }
}

