package hwan.project2.exception;

import java.util.Map;

public record ErrorResponse(String code, String message, Map<String, String> errors) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null);
    }
}

