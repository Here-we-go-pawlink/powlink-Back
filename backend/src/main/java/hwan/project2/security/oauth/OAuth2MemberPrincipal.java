package hwan.project2.security.oauth;

import hwan.project2.domain.member.Member;
import hwan.project2.domain.member.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class OAuth2MemberPrincipal implements OAuth2User {

    private final Long id;
    private final String email;
    private final String displayName;
    private final Role role;
    private final Map<String, Object> attributes;
    private final Collection<? extends GrantedAuthority> authorities;

    private OAuth2MemberPrincipal(Long id,
                                  String email,
                                  String displayName,
                                  Role role,
                                  Map<String, Object> attributes,
                                  Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.attributes = attributes;
        this.authorities = authorities;
    }

    public static OAuth2MemberPrincipal from(Member member, Map<String, Object> attributes) {
        return new OAuth2MemberPrincipal(
                member.getId(),
                member.getEmail(),
                member.displayName(),
                member.getRole(),
                attributes,
                List.of(new SimpleGrantedAuthority(member.getRole().name()))
        );
    }

    @Override
    public String getName() {
        return String.valueOf(id);
    }
}
