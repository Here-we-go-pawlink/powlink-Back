package hwan.project2.domain.member;

import hwan.project2.domain.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "members")
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 별칭 (중복 가능)
    @Column(nullable = false, length = 20)
    private String name;

    // 예: hwan#1234 같은 최종 표시값으로 쓸 거면 정책 다시 정해야 함
    @Column(nullable = false, unique = true, length = 120)
    private String tag;

    // 대표 이메일 느낌. 소셜에서도 쓸 수 있음
    @Column(nullable = false, unique = true, length = 120)
    private String email;

    // 로컬 회원만 값이 있고, 소셜 회원은 null 가능
    @Column(length = 200)
    private String password;

    @Column(length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    public static Member createLocal(String name, String tag, String email, String encodedPassword, String profileImageUrl) {
        Member member = new Member();
        member.name = name;
        member.tag = tag;
        member.email = email;
        member.password = encodedPassword;
        member.profileImageUrl = profileImageUrl;
        member.status = MemberStatus.ACTIVE;
        member.role = Role.ROLE_USER;
        return member;
    }

    public static Member createSocial(String name, String tag, String email, String profileImageUrl) {
        Member member = new Member();
        member.name = name;
        member.tag = tag;
        member.email = email;
        member.password = null;
        member.profileImageUrl = profileImageUrl;
        member.status = MemberStatus.ACTIVE;
        member.role = Role.ROLE_USER;
        return member;
    }

    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public String displayName() {
        return this.name + "#" + this.tag;
    }

    public void changeRole(Role role) {
        this.role = role;
    }

    public void suspend() {
        this.status = MemberStatus.SUSPENDED;
    }

    public void activate() {
        this.status = MemberStatus.ACTIVE;
    }

    public void deleteSoft() {
        this.status = MemberStatus.DELETED;
        this.email = "deleted_" + this.id + "_" + System.currentTimeMillis() + "@deleted.local";
    }

    public boolean isActive() {
        return this.status == MemberStatus.ACTIVE;
    }

    public boolean hasPassword() {
        return this.password != null && !this.password.isBlank();
    }
}