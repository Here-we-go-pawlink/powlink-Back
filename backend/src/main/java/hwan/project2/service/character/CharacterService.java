package hwan.project2.service.character;

import hwan.project2.domain.character.Character;
import hwan.project2.domain.character.repo.CharacterRepository;
import hwan.project2.domain.member.Member;
import hwan.project2.domain.member.repo.MemberRepository;
import hwan.project2.exception.auth.MemberNotFoundException;
import hwan.project2.exception.character.CharacterNotFoundException;
import hwan.project2.web.dto.character.CharacterRequest;
import hwan.project2.web.dto.character.CharacterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CharacterService {

    private final CharacterRepository characterRepository;
    private final MemberRepository memberRepository;

    public CharacterResponse getMyCharacter(Long memberId) {
        return characterRepository.findByMemberId(memberId)
                .map(CharacterResponse::from)
                .orElseThrow(CharacterNotFoundException::new);
    }

    @Transactional
    public CharacterResponse createCharacter(Long memberId, CharacterRequest req) {
        if (characterRepository.existsByMemberId(memberId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 캐릭터가 설정되어 있습니다. PUT으로 수정하세요.");
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        Character character = Character.create(member, req.name(), req.tone(),
                req.personality(), req.musicGenre(), req.activityType());
        return CharacterResponse.from(characterRepository.save(character));
    }

    @Transactional
    public CharacterResponse updateCharacter(Long memberId, CharacterRequest req) {
        Character character = characterRepository.findByMemberId(memberId)
                .orElseThrow(CharacterNotFoundException::new);
        character.update(req.name(), req.tone(), req.personality(), req.musicGenre(), req.activityType());
        return CharacterResponse.from(character);
    }
}
