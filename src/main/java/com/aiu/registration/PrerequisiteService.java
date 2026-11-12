package com.aiu.registration;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class PrerequisiteService {

  private final JdbcTemplate jdbcTemplate;

  public PrerequisiteService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<String> findMissingPrerequisites(long sectionId, String universityId) {

    String sql =
        """
        SELECT DISTINCT p.code
        FROM section s
        JOIN CoursePrerequisite cp
            ON cp.course_id = s.course_id
        JOIN course p
            ON p.course_id = cp.prerequisite_id
        WHERE s.section_id = ?
        AND NOT EXISTS (
            SELECT 1
            FROM CompletedCourse cc
            JOIN `User` u
                ON u.user_id = cc.student_id
            WHERE u.university_id = ?
              AND cc.course_id = cp.prerequisite_id
              AND cc.passed = TRUE
        )
        ORDER BY p.code
        """;

    return jdbcTemplate.queryForList(sql, String.class, sectionId, universityId);
  }
}
