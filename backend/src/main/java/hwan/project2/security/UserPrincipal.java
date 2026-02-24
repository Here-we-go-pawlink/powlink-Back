package hwan.project2.security;

import hwan.project2.domain.member.Role;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
@RequiredArgsConstructor
public class UserPrincipal implements UserDetails {

    private final Long id;                 // DB PK
    private final String email;            // 로그인 식별자
    private final String name;             // 별칭
    private final String tag;              // 태그
    private final String password;         // 해시
    private final Role role;
    private final Collection<? extends GrantedAuthority> authorities;

    public String getDisplayName() {
        return name + "#" + tag;
    }

    // Spring Security의 "username" = 로그인 식별자
    @Override
    public String getUsername() {
        return email;
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
