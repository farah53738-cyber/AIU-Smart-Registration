package com.aiu.registration;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ScheduleService {

  private final JdbcTemplate jdbcTemplate;

  public ScheduleService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public record Meeting(
      long sectionId,
      String courseCode,
      int dayOfWeek,
      LocalTime startTime,
      LocalTime endTime,
      LocalDate startDate,
      LocalDate endDate) {}

  public record Conflict(long firstSectionId, long secondSectionId, String message) {}

  public List<Conflict> checkConflicts(List<Long> sectionIds) {

    if (sectionIds == null || sectionIds.size() < 2) {
      return List.of();
    }

    String sql =
        """
        SELECT
            m.section_id,
            c.code,
            m.day_of_week,
            m.start_time,
            m.end_time,
            m.start_date,
            m.end_date
        FROM SectionMeeting m
        JOIN section s
            ON m.section_id = s.section_id
        JOIN course c
            ON s.course_id = c.course_id
        ORDER BY m.section_id
        """;

    List<Meeting> meetings =
        jdbcTemplate.query(
            sql,
            (rs, rowNum) ->
                new Meeting(
                    rs.getLong("section_id"),
                    rs.getString("code"),
                    rs.getInt("day_of_week"),
                    rs.getTime("start_time").toLocalTime(),
                    rs.getTime("end_time").toLocalTime(),
                    rs.getDate("start_date").toLocalDate(),
                    rs.getDate("end_date").toLocalDate()));

    List<Conflict> conflicts = new ArrayList<>();

    for (int i = 0; i < meetings.size(); i++) {
      Meeting first = meetings.get(i);

      if (!sectionIds.contains(first.sectionId())) {
        continue;
      }

      for (int j = i + 1; j < meetings.size(); j++) {
        Meeting second = meetings.get(j);

        if (!sectionIds.contains(second.sectionId())) {
          continue;
        }

        if (first.sectionId() == second.sectionId()) {
          continue;
        }

        // Classes must occur on the same day.
        boolean sameDay = first.dayOfWeek() == second.dayOfWeek();

        // Their teaching date ranges must overlap.
        boolean datesOverlap =
            !first.endDate().isBefore(second.startDate())
                && !second.endDate().isBefore(first.startDate());

        // Meeting times must overlap.
        boolean timesOverlap =
            first.startTime().isBefore(second.endTime())
                && second.startTime().isBefore(first.endTime());

        if (sameDay && datesOverlap && timesOverlap) {
          String message =
              first.courseCode()
                  + " conflicts with "
                  + second.courseCode()
                  + " on "
                  + dayName(first.dayOfWeek());

          conflicts.add(new Conflict(first.sectionId(), second.sectionId(), message));
        }
      }
    }

    return conflicts;
  }

  private String dayName(int day) {
    return switch (day) {
      case 1 -> "Monday";
      case 2 -> "Tuesday";
      case 3 -> "Wednesday";
      case 4 -> "Thursday";
      case 5 -> "Friday";
      case 6 -> "Saturday";
      case 7 -> "Sunday";
      default -> "Unknown day";
    };
  }
}
