
package com.aiu.registration;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CourseController {

    private final JdbcTemplate jdbcTemplate;

    public CourseController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/courses")
    public List<Map<String, Object>> getCourses() {
        return jdbcTemplate.queryForList(
            "SELECT course_id, code, title, credits " +
            "FROM Course WHERE active = TRUE ORDER BY code"
        );
    }
}
