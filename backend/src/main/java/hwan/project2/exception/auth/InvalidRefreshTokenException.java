package hwan.project2.exception.auth;

public class InvalidRefreshTokenException extends AuthException {
    public InvalidRefreshTokenException() {
        super("Invalid refresh token");
    }
}
