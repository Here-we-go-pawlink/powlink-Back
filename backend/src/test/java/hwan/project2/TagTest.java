package hwan.project2;



import hwan.project2.domain.member.Member;
import hwan.project2.domain.member.repo.MemberRepository;
import hwan.project2.service.auth.AuthService;
import hwan.project2.service.tag.MemberTagGenerator;
import hwan.project2.web.dto.SignupRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TagTest {

    @Autowired
    MemberTagGenerator generator;

    @Autowired
    AuthService authService;
    @Autowired
    MemberRepository memberRepository;


    @Test
    void generateUniqueTag_shouldCreateDifferentValues() {
        Set<String> tags = new HashSet<>();

        for (int i = 0; i < 200; i++) {
            String tag = generator.generateUniqueTag();
            assertNotNull(tag);
            assertTrue(tag.length() >= 4 && tag.length() <= 6);
            assertTrue(tags.add(tag)); // 중복이면 false
        }
    }

    @Test
    void 회원가입_태그를이용해서() {
        Long id = authService.signup(new SignupRequest("hwan", "hwan@test.com", "pw1234"));

        Member member = memberRepository.findById(id).orElseThrow();
        assertEquals("hwan", member.getName());
        assertNotNull(member.getTag());
        assertFalse(member.getTag().isBlank());
        assertEquals("hwan@test.com", member.getEmail());
        assertTrue(member.isActive());
        assertNotNull(member.getCreatedAt());
        assertNotNull(member.getUpdatedAt());

    }
}

