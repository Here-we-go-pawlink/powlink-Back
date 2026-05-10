package hwan.project2.domain.community;

public enum ReactionType {
    EMPATHY, COMFORT, UNDERSTAND;

    public static ReactionType from(String value) {
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid reaction type: " + value);
        }
    }
}
