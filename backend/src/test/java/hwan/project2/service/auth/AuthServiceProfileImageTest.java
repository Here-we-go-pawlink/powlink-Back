package hwan.project2.service.auth;

import hwan.project2.domain.member.Member;
import hwan.project2.domain.member.repo.MemberRepository;
import hwan.project2.web.dto.SignupRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AuthServiceProfileImageTest {

    @Autowired
    AuthService authService;

    @Autowired
    MemberRepository memberRepository;

    @MockitoBean
    StringRedisTemplate redis;

    @Test
    @DisplayName("프로필 이미지 URL과 함께 회원가입하면 저장된다")
    void signup_withProfileImageUrl_savesUrl() {
        // given
        String profileImageUrl = "/images/profile/abc123.jpg";

        // when
        Long memberId = authService.signup(
                new SignupRequest("tester", "tester@test.com", "password1234", profileImageUrl)
        );

        // then
        Member member = memberRepository.findById(memberId).orElseThrow();
        assertThat(member.getProfileImageUrl()).isEqualTo(profileImageUrl);
    }

    @Test
    @DisplayName("프로필 이미지 없이 회원가입하면 null로 저장된다")
    void signup_withoutProfileImageUrl_savesNull() {
        Long memberId = authService.signup(
                new SignupRequest("tester", "tester@test.com", "password1234", null)
        );

        Member member = memberRepository.findById(memberId).orElseThrow();
        assertThat(member.getProfileImageUrl()).isNull();
    }

    @Test
    @DisplayName("프로필 이미지를 나중에 변경할 수 있다")
    void updateProfileImage_changesUrl() {
        // given
        Long memberId = authService.signup(
                new SignupRequest("tester", "tester@test.com", "password1234", "/images/profile/old.jpg")
        );
        Member member = memberRepository.findById(memberId).orElseThrow();

        // when
        member.updateProfileImage("/images/profile/new.jpg");

        // then
        assertThat(member.getProfileImageUrl()).isEqualTo("/images/profile/new.jpg");
    }
}
