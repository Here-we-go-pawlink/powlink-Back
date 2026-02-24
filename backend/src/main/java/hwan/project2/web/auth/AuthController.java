package hwan.project2.web.auth;

import hwan.project2.security.UserPrincipal;
import hwan.project2.service.auth.AuthService;
import hwan.project2.web.dto.*;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;


    @PostMapping("/signup")
    public Long signup(@Valid @RequestBody SignupRequest req) {
        return authService.signup(req);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req);
    }



    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader("Authorization") String authHeader
    ) {
        if (principal == null || authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String accessToken = authHeader.substring(7);
        authService.logout(principal.getId(), accessToken);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return new MeResponse(
                principal.getId(),
                principal.getEmail(),
                principal.getDisplayName(),
                principal.getRole().name()
        );
    }


}

