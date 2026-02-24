package hwan.project2.service.auth;

import hwan.project2.domain.member.Member;
import hwan.project2.domain.member.repo.MemberRepository;
import hwan.project2.exception.auth.InvalidCredentialsException;
import hwan.project2.exception.auth.InvalidRefreshTokenException;
import hwan.project2.exception.auth.MemberNotFoundException;
import hwan.project2.security.jwt.JwtTokenProvider;
import hwan.project2.service.tag.MemberTagGenerator;
import hwan.project2.web.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final MemberTagGenerator tagGenerator;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redis;
    private final long refreshExpSeconds;

    public AuthService(MemberRepository memberRepository,
                       MemberTagGenerator tagGenerator,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       StringRedisTemplate redis,
                       @Value("${jwt.refresh-exp-seconds}") long refreshExpSeconds) {
        this.memberRepository = memberRepository;
        this.tagGenerator = tagGenerator;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redis = redis;
        this.refreshExpSeconds = refreshExpSeconds;
    }


    @Transactional
    public Long signup(SignupRequest req) {
        if (memberRepository.existsByEmail(req.email())) {
            throw new hwan.project2.exception.auth.MemberAlreadyExistsException("email already exists");
        }

        try {
            String tag = tagGenerator.generateUniqueTag();
            Member member = Member.create(req.name(), tag, req.email(), passwordEncoder.encode(req.password()));

            Member saved = memberRepository.save(member);
            log.info("signup ok. name={}, email={}, tag={}", saved.getName(), saved.getEmail(), saved.getTag());
            return saved.getId();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Signup conflict (tag or email): {}", e.getMessage());
            throw new hwan.project2.exception.auth.MemberAlreadyExistsException("User with this email or tag already exists");
        }
    }

    public TokenResponse login(LoginRequest req) {
        Member member = memberRepository.findByEmail(req.email())
                .orElseThrow(InvalidCredentialsException::new);

        // ✅ 정보 노출 줄이려면 비활성도 로그인 실패로 뭉개는 게 보통 더 안전
        if (!member.isActive()) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(req.password(), member.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String access = jwtTokenProvider.createAccessToken(member.getId(), member.getRole());
        String refresh = jwtTokenProvider.createRefreshToken(member.getId());

        // ✅ Redis TTL = 설정값 기반으로 통일
        redis.opsForValue().set(refreshKey(member.getId()), refresh, refreshTtl());

        return new TokenResponse(access, refresh);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest req) {
        String refreshToken = req.refreshToken();

        if (!jwtTokenProvider.validate(refreshToken)) {
            throw new InvalidRefreshTokenException();
        }

        Long memberId = jwtTokenProvider.getMemberId(refreshToken);
        String key = refreshKey(memberId);
        String saved = redis.opsForValue().get(key);

        if (saved == null || !saved.equals(refreshToken)) {
            throw new InvalidRefreshTokenException();
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        if (!member.isActive()) {
            throw new InvalidRefreshTokenException();
        }

        String newAccess = jwtTokenProvider.createAccessToken(memberId, member.getRole());
        String newRefresh = jwtTokenProvider.createRefreshToken(memberId);

        redis.opsForValue().set(key, newRefresh, refreshTtl());

        return new TokenResponse(newAccess, newRefresh);
    }

    @Transactional
    public void logout(Long memberId, String accessToken) {
        // 1. Refresh Token 삭제
        redis.delete(refreshKey(memberId));

        // 2. Access Token 블랙리스트 등록 (남은 유효시간만큼)
        // 실제 운영에서는 jwtTokenProvider에서 남은 시간을 계산해서 가져오는 것이 좋음
        long remainMs = jwtTokenProvider.getRemainingExpirationMs(accessToken);
        if (remainMs > 0) {
            redis.opsForValue().set("BL:" + accessToken, "logout", Duration.ofMillis(remainMs));
        }
    }

    private String refreshKey(Long memberId) {
        return "RT:" + memberId;
    }

    private Duration refreshTtl() {
        return Duration.ofSeconds(refreshExpSeconds);
    }
}
