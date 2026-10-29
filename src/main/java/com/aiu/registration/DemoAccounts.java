package com.aiu.registration;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

public final class DemoAccounts {

    private DemoAccounts() {
    }

    public record Student(String studentId, String studentName) {
    }

    public static Student student(Authentication authentication) {

        return switch (authentication.getName()) {
            case "student1" -> new Student("1", "Sarah");
            case "student2" -> new Student("2", "Demo Student");

            default -> throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Student account required."
            );
        };
    }

    public static boolean isAdvisor(Authentication authentication) {

        return authentication.getAuthorities().stream()
            .anyMatch(authority ->
                authority.getAuthority().equals("ROLE_ADVISOR")
            );
    }
}