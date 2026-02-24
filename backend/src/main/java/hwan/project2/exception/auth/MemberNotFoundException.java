package hwan.project2.exception.auth;

public class MemberNotFoundException extends AuthException {
    public MemberNotFoundException() {
        super("Member not found");
    }
}
