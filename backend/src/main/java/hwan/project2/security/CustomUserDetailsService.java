package hwan.project2.security;

import hwan.project2.domain.member.Member;
import hwan.project2.domain.member.repo.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("loadUserByUsername: {}", email);
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Member not found: " + email));

        return toPrincipal(member);
    }

    public UserDetails loadUserById(Long memberId) throws UsernameNotFoundException {
        log.debug("loadUserById: {}", memberId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new UsernameNotFoundException("Member not found id: " + memberId));

        return toPrincipal(member);
    }

    private UserDetails toPrincipal(Member member) {
        return new UserPrincipal(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getTag(),
                member.getPassword(),
                member.getRole(),
                List.of(new SimpleGrantedAuthority(member.getRole().name()))
        );
    }

}
