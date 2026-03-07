package hwan.project2.web.test;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication == null) {
            return "not login";
        }
        return "login success : " + authentication.getName();
    }
}
