package hwan.project2.security.jwt;

import hwan.project2.domain.member.Role;
import hwan.project2.security.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final org.springframework.data.redis.core.StringRedisTemplate redis;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   CustomUserDetailsService userDetailsService,
                                   org.springframework.data.redis.core.StringRedisTemplate redis) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.redis = redis;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        log.debug("[JWT] uri={}", request.getRequestURI());

        // 이미 인증 있으면 스킵
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        // ✅ 블랙리스트 체크
        if (Boolean.TRUE.equals(redis.hasKey("BL:" + token))) {
            log.debug("[JWT] blacklisted token");
            chain.doFilter(request, response);
            return;
        }

        Claims claims;
        try {
            // 파싱/서명/만료 검증: 1번만
            claims = jwtTokenProvider.parseAndValidate(token);
        } catch (Exception e) {
            log.debug("[JWT] invalid token: {}", e.getMessage());
            chain.doFilter(request, response);
            return;
        }

        // AccessToken만 인증: role 없으면(refresh) 인증 세팅 안 함
        Role role = jwtTokenProvider.getRole(claims);
        if (role == null) {
            log.debug("[JWT] no role in token (refresh token)");
            chain.doFilter(request, response);
            return;
        }

        Long memberId = jwtTokenProvider.getMemberId(claims);
        if (memberId == null) {
            log.debug("[JWT] no/invalid subject in token");
            chain.doFilter(request, response);
            return;
        }

        log.debug("[JWT] memberId={}, role={}", memberId, role);

        UserDetails userDetails = userDetailsService.loadUserById(memberId);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );

        // (선택이지만 추천) 요청 정보(IP 등) 세팅
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(request, response);
    }
}
