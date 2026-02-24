package hwan.project2.config;

import hwan.project2.exception.ErrorResponse;
import hwan.project2.security.CustomUserDetailsService;
import hwan.project2.security.jwt.JwtAuthenticationFilter;
import hwan.project2.security.jwt.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableMethodSecurity
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtTokenProvider jwtTokenProvider,
            CustomUserDetailsService customUserDetailsService,
            ObjectMapper objectMapper,
            org.springframework.data.redis.core.StringRedisTemplate redis
    ) throws Exception {

        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/signup", "/api/auth/login", "/api/auth/refresh").permitAll()
                .requestMatchers("/api/auth/logout", "/api/auth/me").authenticated()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
        );

        http.addFilterBefore(
                new JwtAuthenticationFilter(jwtTokenProvider, customUserDetailsService, redis),
                UsernamePasswordAuthenticationFilter.class
        );

        http.exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(401);
                    res.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);

                    ErrorResponse body = ErrorResponse.of("UNAUTHORIZED", "Unauthorized");
                    objectMapper.writeValue(res.getWriter(), body);
                })
                .accessDeniedHandler((req, res, ex) -> {
                    res.setStatus(403);
                    res.setCharacterEncoding(StandardCharsets.UTF_8.name());
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);

                    ErrorResponse body = ErrorResponse.of("FORBIDDEN", "Forbidden");
                    objectMapper.writeValue(res.getWriter(), body);
                })
        );

        return http.build();
    }
}
