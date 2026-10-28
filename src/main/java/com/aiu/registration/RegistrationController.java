package com.aiu.registration;

import java.util.List;
import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class RegistrationController {

    private final JdbcTemplate jdbcTemplate;

    public RegistrationController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/requests")
    public List<Map<String, Object>> getRequests() {
        return jdbcTemplate.queryForList(
            "SELECT r.request_id, r.student_name, r.student_id, " +
            "r.status, r.requested_at, c.code, c.title, " +
            "s.section_number, s.schedule " +
            "FROM RegistrationRequest r " +
            "JOIN Section s ON r.section_id = s.section_id " +
            "JOIN Course c ON s.course_id = c.course_id " +
            "ORDER BY r.request_id DESC"
        );
    }

    @PostMapping("/api/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> createRequest(@RequestBody RequestData request) {
        if (request.studentName() == null ||
            request.studentName().isBlank() ||
            request.studentName().trim().length() > 100 ||
            request.studentId() == null ||
            request.studentId().isBlank() ||
            request.studentId().trim().length() > 20 ||
            request.sectionId() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Please enter a valid name, student ID and section."
            );
        }

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM Section WHERE section_id = ?",
            Integer.class,
            request.sectionId()
        );

        if (count == null || count == 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "The selected section does not exist."
            );
        }

        jdbcTemplate.update(
            "INSERT INTO RegistrationRequest " +
            "(student_name, student_id, section_id) VALUES (?, ?, ?)",
            request.studentName().trim(),
            request.studentId().trim(),
            request.sectionId()
        );

        return Map.of("message", "Registration request submitted.");
    }

    public record RequestData(
        String studentName,
        String studentId,
        Long sectionId
    ) {}
    
    
    @GetMapping("/api/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }
    
    
}