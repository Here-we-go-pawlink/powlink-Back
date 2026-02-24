package hwan.project2.security.jwt;

import hwan.project2.domain.member.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final Key key;
    private final long accessExpSeconds;
    private final long refreshExpSeconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-exp-seconds}") long accessExpSeconds,
            @Value("${jwt.refresh-exp-seconds}") long refreshExpSeconds
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpSeconds = accessExpSeconds;
        this.refreshExpSeconds = refreshExpSeconds;
    }

    public String createAccessToken(Long memberId, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(String.valueOf(memberId))
                .claim("role", role.name())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(accessExpSeconds)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(Long memberId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(String.valueOf(memberId))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(refreshExpSeconds)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 파싱 + 서명검증 + 만료검증을 1번에 */
    public Claims parseAndValidate(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /** Claims 기반: 필터가 토큰 스키마를 몰라도 되게 */
    public Long getMemberId(Claims claims) {
        try {
            return Long.valueOf(claims.getSubject());
        } catch (RuntimeException e) {
            return null;
        }
    }

    public Role getRole(Claims claims) {
        Object role = claims.get("role");
        if (role == null) return null;

        try {
            return Role.valueOf(String.valueOf(role));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public long getRemainingExpirationMs(String token) {
        try {
            Date expiration = parseAndValidate(token).getExpiration();
            return expiration.getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            return 0;
        }
    }

    // (선택) 기존 코드 호환용. 새 필터에서는 Claims 기반 메서드를 쓰면 됨.
    public boolean validate(String token) {
        try {
            parseAndValidate(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Deprecated
    public Claims getClaims(String token) {
        return parseAndValidate(token);
    }

    @Deprecated
    public Long getMemberId(String token) {
        return Long.valueOf(parseAndValidate(token).getSubject());
    }

    @Deprecated
    public Role getRole(String token) {
        Object role = parseAndValidate(token).get("role");
        return role == null ? null : Role.valueOf(String.valueOf(role));
    }
}
