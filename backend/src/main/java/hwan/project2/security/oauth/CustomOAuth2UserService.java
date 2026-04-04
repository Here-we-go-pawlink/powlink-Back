package hwan.project2.security.oauth;

import hwan.project2.domain.member.Member;
import hwan.project2.domain.member.SocialAccount;
import hwan.project2.domain.member.SocialProvider;
import hwan.project2.domain.member.repo.MemberRepository;
import hwan.project2.domain.member.repo.SocialAccountRepository;
import hwan.project2.service.tag.MemberTagGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final int MEMBER_NAME_MAX_LEN = 20;

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final SocialAccountRepository socialAccountRepository;
    private final MemberRepository memberRepository;
    private final MemberTagGenerator tagGenerator;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("[OAUTH2] provider={}, rawAttributes={}", registrationId, oauth2User.getAttributes());
        return loadOrCreateMemberFromAttributes(registrationId, oauth2User.getAttributes());
    }

    @Transactional
    public OAuth2MemberPrincipal loadOrCreateMemberFromAttributes(String registrationId, Map<String, Object> attributes) {
        SocialProfile profile = SocialProfile.from(registrationId, attributes);
        log.info(
                "[OAUTH2] parsed provider={}, providerUserId={}, email={}, name={}",
                profile.provider(),
                profile.providerUserId(),
                profile.email(),
                profile.name()
        );
        return loadOrCreateMemberForSocial(
                profile.provider(),
                profile.providerUserId(),
                profile.email(),
                profile.name(),
                attributes
        );
    }

    OAuth2MemberPrincipal loadOrCreateMemberForSocial(SocialProvider provider,
                                                      String providerUserId,
                                                      String email,
                                                      String name,
                                                      Map<String, Object> attributes) {
        SocialProfile profile = new SocialProfile(provider, providerUserId, email, name);
        SocialAccount socialAccount = socialAccountRepository
                .findByProviderAndProviderUserId(profile.provider(), profile.providerUserId())
                .orElseGet(() -> createSocialAccount(profile));

        return OAuth2MemberPrincipal.from(socialAccount.getMember(), attributes);
    }

    private SocialAccount createSocialAccount(SocialProfile profile) {
        String tag = tagGenerator.generateUniqueTag();
        String displayName = buildDisplayName(profile);

        // No auto-merge by email: use provider+id based deterministic internal email.
        String internalEmail = buildInternalSocialEmail(profile.provider(), profile.providerUserId());
        Member member = Member.createSocial(displayName, tag, internalEmail, null);
        Member savedMember = memberRepository.save(member);

        SocialAccount socialAccount = SocialAccount.create(
                savedMember,
                profile.provider(),
                profile.providerUserId(),
                profile.email(),
                profile.name()
        );
        return socialAccountRepository.save(socialAccount);
    }

    private String buildDisplayName(SocialProfile profile) {
        String candidate = profile.name();
        if (candidate == null || candidate.isBlank()) {
            candidate = profile.provider().name().toLowerCase(Locale.ROOT)
                    + "_"
                    + profile.providerUserId().substring(0, Math.min(6, profile.providerUserId().length()));
        }
        String normalized = candidate.trim();
        if (normalized.length() > MEMBER_NAME_MAX_LEN) {
            return normalized.substring(0, MEMBER_NAME_MAX_LEN);
        }
        return normalized;
    }

    private String buildInternalSocialEmail(SocialProvider provider, String providerUserId) {
        String raw = provider.name() + ":" + providerUserId;
        String digest = sha256Hex(raw).substring(0, 24);
        return provider.name().toLowerCase(Locale.ROOT) + "_" + digest + "@social.local";
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private record SocialProfile(SocialProvider provider, String providerUserId, String email, String name) {
        private static SocialProfile from(String registrationId, Map<String, Object> attributes) {
            String normalized = registrationId.toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "google" -> fromGoogle(attributes);
                case "naver" -> fromNaver(attributes);
                case "kakao" -> fromKakao(attributes);
                default -> throw oauth2Error("Unsupported provider: " + registrationId);
            };
        }

        private static SocialProfile fromGoogle(Map<String, Object> attributes) {
            String providerUserId = asString(attributes.get("sub"));
            String email = asString(attributes.get("email"));
            String name = asString(attributes.get("name"));
            validateProviderUserId(providerUserId, "google");
            return new SocialProfile(SocialProvider.GOOGLE, providerUserId, email, name);
        }

        @SuppressWarnings("unchecked")
        private static SocialProfile fromNaver(Map<String, Object> attributes) {
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            if (response == null) {
                throw oauth2Error("Missing naver response attributes");
            }
            String providerUserId = asString(response.get("id"));
            String email = asString(response.get("email"));
            String name = asString(response.get("name"));
            if (name == null || name.isBlank()) {
                name = asString(response.get("nickname"));
            }
            validateProviderUserId(providerUserId, "naver");
            return new SocialProfile(SocialProvider.NAVER, providerUserId, email, name);
        }

        @SuppressWarnings("unchecked")
        private static SocialProfile fromKakao(Map<String, Object> attributes) {
            String providerUserId = asString(attributes.get("id"));
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            String email = null;
            String name = null;
            if (kakaoAccount != null) {
                email = asString(kakaoAccount.get("email"));
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null) {
                    name = asString(profile.get("nickname"));
                }
            }
            validateProviderUserId(providerUserId, "kakao");
            return new SocialProfile(SocialProvider.KAKAO, providerUserId, email, name);
        }

        private static String asString(Object value) {
            return value == null ? null : String.valueOf(value);
        }

        private static void validateProviderUserId(String providerUserId, String provider) {
            if (providerUserId == null || providerUserId.isBlank()) {
                throw oauth2Error("Missing provider user id: " + provider);
            }
        }

        private static OAuth2AuthenticationException oauth2Error(String message) {
            OAuth2Error error = new OAuth2Error("oauth2_user_invalid", message, null);
            return new OAuth2AuthenticationException(error, message);
        }
    }
}
