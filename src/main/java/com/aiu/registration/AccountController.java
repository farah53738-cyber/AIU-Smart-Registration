package com.aiu.registration;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController {

    @GetMapping("/api/me")
    public Map<String, String> me(Authentication authentication) {

        if (DemoAccounts.isAdvisor(authentication)) {
            return Map.of(
                "username", authentication.getName(),
                "role", "ADVISOR",
                "displayName", "Demo Advisor"
            );
        }

        DemoAccounts.Student student =
            DemoAccounts.student(authentication);

        return Map.of(
            "username", authentication.getName(),
            "role", "STUDENT",
            "displayName", student.studentName(),
            "studentId", student.studentId()
        );
    }
}