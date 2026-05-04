package hwan.project2.exception.letter;

public class LetterNotFoundException extends RuntimeException {
    public LetterNotFoundException() {
        super("Letter not found");
    }
}
