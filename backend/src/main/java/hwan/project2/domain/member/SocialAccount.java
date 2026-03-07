package hwan.project2.domain.member;

import hwan.project2.domain.base.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "social_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_provider_provider_user_id",
                        columnNames = {"provider", "provider_user_id"}
                )
        }
)
public class SocialAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 여러 소셜 로그인 수단이 하나의 Member에 연결될 수 있음
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SocialProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 120)
    private String providerUserId;

    @Column(length = 120)
    private String socialEmail;

    @Column(length = 120)
    private String socialName;

    protected SocialAccount(Member member, SocialProvider provider, String providerUserId,
                            String socialEmail, String socialName) {
        this.member = member;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.socialEmail = socialEmail;
        this.socialName = socialName;
    }

    public static SocialAccount create(Member member, SocialProvider provider, String providerUserId,
                                       String socialEmail, String socialName) {
        return new SocialAccount(member, provider, providerUserId, socialEmail, socialName);
    }
}