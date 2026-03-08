package hwan.project2.security.oauth;

import hwan.project2.domain.member.Member;
import hwan.project2.domain.member.SocialProvider;
import hwan.project2.domain.member.repo.MemberRepository;
import hwan.project2.domain.member.repo.SocialAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class CustomOAuth2UserServiceTest {

    @Autowired
    CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    SocialAccountRepository socialAccountRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("소셜 로그인 시 같은 이메일 로컬 계정이 있어도 자동 병합하지 않고 신규 회원을 만든다")
    void socialLogin_shouldNotMergeByEmail() {
        Member local = Member.createLocal("localUser", "LOCAL1", "same@test.com", passwordEncoder.encode("password123"));
        memberRepository.save(local);
        long beforeMemberCount = memberRepository.count();

        OAuth2MemberPrincipal principal = customOAuth2UserService.loadOrCreateMemberForSocial(
                SocialProvider.GOOGLE,
                "google-sub-001",
                "same@test.com",
                "Google User",
                Map.of("sub", "google-sub-001")
        );

        long afterMemberCount = memberRepository.count();
        assertEquals(beforeMemberCount + 1, afterMemberCount);
        assertNotEquals(local.getId(), principal.getId());
        assertEquals(1, socialAccountRepository.count());
    }

    @Test
    @DisplayName("같은 provider + providerUserId 로 재로그인하면 기존 회원을 재사용한다")
    void socialLogin_shouldReuseExistingMemberByProviderKey() {
        OAuth2MemberPrincipal first = customOAuth2UserService.loadOrCreateMemberForSocial(
                SocialProvider.GOOGLE,
                "google-sub-002",
                "user@test.com",
                "Google User2",
                Map.of("sub", "google-sub-002")
        );

        long memberCountAfterFirst = memberRepository.count();
        long socialCountAfterFirst = socialAccountRepository.count();

        OAuth2MemberPrincipal second = customOAuth2UserService.loadOrCreateMemberForSocial(
                SocialProvider.GOOGLE,
                "google-sub-002",
                "changed@test.com",
                "Changed Name",
                Map.of("sub", "google-sub-002")
        );

        assertEquals(first.getId(), second.getId());
        assertEquals(memberCountAfterFirst, memberRepository.count());
        assertEquals(socialCountAfterFirst, socialAccountRepository.count());
    }

    @Test
    @DisplayName("소셜 이름이 길어도 멤버 이름 길이 제한(20자) 내로 저장된다")
    void socialLogin_shouldTrimLongNameToMemberLimit() {
        OAuth2MemberPrincipal principal = customOAuth2UserService.loadOrCreateMemberForSocial(
                SocialProvider.GOOGLE,
                "google-sub-003",
                "longname@test.com",
                "This Name Is Definitely Longer Than Twenty Characters",
                Map.of("sub", "google-sub-003")
        );

        Member saved = memberRepository.findById(principal.getId()).orElseThrow();
        assertTrue(saved.getName().length() <= 20);
    }
}
