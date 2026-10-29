package com.aiu.registration;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class RegistrationController {

    private final JdbcTemplate jdbcTemplate;

    public RegistrationController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String REQUEST_QUERY =
        "SELECT r.request_id, r.student_name, r.student_id, " +
        "r.status, r.requested_at, c.code, c.title, " +
        "s.section_number, s.schedule " +
        "FROM registrationrequest r " +
        "JOIN section s ON r.section_id = s.section_id " +
        "JOIN course c ON s.course_id = c.course_id ";

    @GetMapping("/api/requests")
    public List<Map<String, Object>> getRequests(
            Authentication authentication) {

        if (DemoAccounts.isAdvisor(authentication)) {
            return jdbcTemplate.queryForList(
                REQUEST_QUERY + "ORDER BY r.request_id DESC"
            );
        }

        DemoAccounts.Student student =
            DemoAccounts.student(authentication);

        return jdbcTemplate.queryForList(
            REQUEST_QUERY +
            "WHERE r.student_id = ? ORDER BY r.request_id DESC",
            student.studentId()
        );
    }

    @PostMapping("/api/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> createRequest(
            @RequestBody RequestData request,
            Authentication authentication) {

        DemoAccounts.Student student =
            DemoAccounts.student(authentication);

        if (request.sectionId() == null || request.sectionId() <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Choose a valid class section."
            );
        }

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM section s " +
            "JOIN course c ON s.course_id = c.course_id " +
            "WHERE s.section_id = ? AND c.active = TRUE",
            Integer.class,
            request.sectionId()
        );

        if (count == null || count == 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "The selected section is not available."
            );
        }

        Integer duplicate = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM registrationrequest " +
            "WHERE student_id = ? AND section_id = ? " +
            "AND status IN ('PENDING', 'APPROVED')",
            Integer.class,
            student.studentId(),
            request.sectionId()
        );

        if (duplicate != null && duplicate > 0) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "You already have a pending or approved request " +
                "for this section."
            );
        }

        jdbcTemplate.update(
            "INSERT INTO registrationrequest " +
            "(student_name, student_id, section_id) VALUES (?, ?, ?)",
            student.studentName(),
            student.studentId(),
            request.sectionId()
        );

        return Map.of(
            "message", "Registration request submitted.",
            "status", "PENDING"
        );
    }

    @PatchMapping("/api/requests/{id}/status")
    public Map<String, String> decide(
            @PathVariable("id") long id,
            @RequestBody Decision decision,
            Authentication authentication) {

        if (!DemoAccounts.isAdvisor(authentication)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Advisor account required."
            );
        }

        if (id <= 0 || decision.status() == null ||
            !(decision.status().equals("APPROVED") ||
              decision.status().equals("REJECTED"))) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Choose APPROVED or REJECTED."
            );
        }

        int changed = jdbcTemplate.update(
            "UPDATE registrationrequest SET status = ? " +
            "WHERE request_id = ? AND status = 'PENDING'",
            decision.status(),
            id
        );

        if (changed == 0) {

            Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM registrationrequest " +
                "WHERE request_id = ?",
                Integer.class,
                id
            );

            if (exists == null || exists == 0) {
                throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Request not found."
                );
            }

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "This request has already been decided. " +
                "Refresh the list."
            );
        }

        return Map.of(
            "message", "Decision saved.",
            "status", decision.status()
        );
    }

    @GetMapping("/api/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }

    public record RequestData(
        String studentName,
        String studentId,
        Long sectionId
    ) {
    }

    public record Decision(String status) {
    }
}