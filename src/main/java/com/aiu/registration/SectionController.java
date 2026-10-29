package com.aiu.registration;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SectionController {

    private final JdbcTemplate jdbcTemplate;

    public SectionController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/sections")
    public List<Map<String, Object>> getSections() {
        return jdbcTemplate.queryForList(
            "SELECT s.section_id, c.code, c.title, c.credits, " +
            "s.section_number, s.instructor, s.schedule, s.room, s.capacity " +
            "FROM section s " +
            "JOIN course c ON s.course_id = c.course_id " +
            "WHERE c.active = TRUE " +
            "ORDER BY c.code"
        );
    }
}