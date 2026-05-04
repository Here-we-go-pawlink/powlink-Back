package hwan.project2.config;

import hwan.project2.domain.member.Member;
import hwan.project2.domain.member.repo.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * dev/default 프로파일에서만 실행되는 테스트 유저 초기화.
 * 서버 시작 시 test@emolens.com / test1234 계정이 없으면 자동 생성.
 */
@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DevDataInitializer implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (memberRepository.findByEmail("test@emolens.com").isPresent()) {
            return;
        }

        Member testUser = Member.createLocal(
                "테스터",
                "test0001",
                "test@emolens.com",
                passwordEncoder.encode("test1234"),
                null
        );
        memberRepository.save(testUser);
        log.info("[DevDataInitializer] 테스트 계정 생성: test@emolens.com / test1234");
    }
}
