package hwan.project2.domain.member;

import hwan.project2.domain.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 별칭 (중복 가능)
    @Column(nullable = false, length = 20)
    private String name;

    // name#tag
    @Column(nullable = false, unique = true, length = 120)
    private String tag;

    // 로그인 식별자
    @Column(nullable = false, unique = true, length = 120)
    private String email;

    // bcrypt 해시 저장
    @Column(nullable = false, length = 200)
    private String password;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role; // USER or ADMIN


    // ===== 생성 메서드 =====
    public static Member create(String name, String tag, String email, String encodedPassword) {
        Member member = new Member();
        member.name = name;
        member.tag = tag;
        member.email = email;
        member.password = encodedPassword;
        member.status = MemberStatus.ACTIVE;
        member.role = Role.ROLE_USER;
        return member;
    }

    // ===== 도메인 메서드 =====
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
}
