package hwan.project2.service.auth;

import hwan.project2.domain.member.Member;
import hwan.project2.domain.member.repo.MemberRepository;
import hwan.project2.exception.auth.InvalidRefreshTokenException;
import hwan.project2.security.jwt.JwtTokenProvider;
import hwan.project2.web.dto.RefreshRequest;
import hwan.project2.web.dto.SignupRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class AuthServiceRefreshTest {

    @Autowired
    AuthService authService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    StringRedisTemplate redis;

    @MockitoBean
    ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("비활성화된 회원은 리프레시 토큰으로 갱신할 수 없다")
    void refresh_shouldThrowException_whenMemberIsInactive() {
        // given: 회원 가입 및 정지 처리
        Long memberId = authService.signup(new SignupRequest("tester", "tester@test.com", "password"));
        Member member = memberRepository.findById(memberId).orElseThrow();
        member.suspend(); // 비활성화
        memberRepository.saveAndFlush(member);

        // 리프레시 토큰 생성
        String refreshToken = jwtTokenProvider.createRefreshToken(memberId);
        
        // Redis Mock 설정
        BDDMockito.given(redis.opsForValue()).willReturn(valueOperations);
        BDDMockito.given(valueOperations.get("RT:" + memberId)).willReturn(refreshToken);

        // when & then: 리프레시 요청 시 예외 발생 확인
        RefreshRequest req = new RefreshRequest(refreshToken);
        assertThrows(InvalidRefreshTokenException.class, () -> {
            authService.refresh(req);
        });
    }
}
