package hwan.project2.service.tag;

import hwan.project2.domain.member.repo.MemberRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class MemberTagGenerator {

    private static final char[] CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final int TAG_LEN = 5; // 4~6 추천, 나는 5 추천
    private static final int MAX_TRY = 20;

    private final SecureRandom random = new SecureRandom();
    private final MemberRepository memberRepository;

    public MemberTagGenerator(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public String generateUniqueTag() {
        for (int i = 0; i < MAX_TRY; i++) {
            String tag = randomTag();
            if (!memberRepository.existsByTag(tag)) {
                return tag;
            }
        }
        throw new IllegalStateException("태그 생성에 반복 실패했습니다. (충돌 과다)");
    }

    private String randomTag() {
        StringBuilder sb = new StringBuilder(TAG_LEN);
        for (int i = 0; i < TAG_LEN; i++) {
            sb.append(CHARS[random.nextInt(CHARS.length)]);
        }
        return sb.toString();
    }
}

