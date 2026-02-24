package hwan.project2.domain.member.repo;

import hwan.project2.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByTag(String tag);
    boolean existsByEmail(String email);
    Optional<Member>  findByEmail(String email);
}

