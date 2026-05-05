package hwan.project2.exception.character;

public class CharacterNotFoundException extends RuntimeException {
    public CharacterNotFoundException() {
        super("캐릭터가 아직 설정되지 않았습니다.");
    }
}
