package com.nitin.saas;

import com.nitin.saas.common.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/me")
    public String me(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return "Logged in as: " + principal.getUsername()
                + " (ID=" + principal.getUserId() + ")";
    }
}

