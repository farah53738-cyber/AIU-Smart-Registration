package com.aiu.registration;

import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class SectionController {
  final BetaService s;

  public SectionController(BetaService s) {
    this.s = s;
  }

  @GetMapping("/api/sections")
  public Object sections(
      @RequestParam(required = false) Long termId,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String instructor,
      @RequestParam(required = false) Integer day,
      @RequestParam(defaultValue = "false") boolean available,
      @RequestParam(required = false) String startsAfter,
      @RequestParam(required = false) String endsBefore,
      @RequestParam(defaultValue = "0") int page) {
    BetaService.require(page >= 0 && page <= 10000, "Invalid page.");
    String sql =
        "SELECT s.*,c.code,c.title,c.credits,t.name AS term_name,(SELECT COUNT(*) FROM Enrollment e"
            + " WHERE e.section_id=s.section_id AND e.status='ACTIVE') AS enrolled FROM section s"
            + " JOIN course c ON c.course_id=s.course_id JOIN `Term` t ON t.term_id=s.term_id WHERE"
            + " c.active=TRUE AND s.active=TRUE";
    var args = new ArrayList<Object>();
    if (termId != null) {
      sql += " AND s.term_id=?";
      args.add(termId);
    }
    sql += " AND (LOWER(c.code) LIKE ? OR LOWER(c.title) LIKE ?) AND LOWER(s.instructor) LIKE ?";
    args.add("%" + q.toLowerCase() + "%");
    args.add("%" + q.toLowerCase() + "%");
    args.add("%" + instructor.toLowerCase() + "%");
    if (day != null || startsAfter != null || endsBefore != null) {
      sql += " AND EXISTS(SELECT 1 FROM SectionMeeting m WHERE m.section_id=s.section_id";
      if (day != null) {
        BetaService.require(day >= 1 && day <= 7, "Invalid day.");
        sql += " AND m.day_of_week=?";
        args.add(day);
      }
      java.time.LocalTime start = null, end = null;
      try {
        if (startsAfter != null) {
          start = java.time.LocalTime.parse(startsAfter);
          sql += " AND m.start_time>=?";
          args.add(java.sql.Time.valueOf(start));
        }
        if (endsBefore != null) {
          end = java.time.LocalTime.parse(endsBefore);
          sql += " AND m.end_time<=?";
          args.add(java.sql.Time.valueOf(end));
        }
      } catch (java.time.format.DateTimeParseException e) {
        BetaService.require(false, "Invalid time.");
      }
      BetaService.require(
          start == null || end == null || start.isBefore(end), "Start must be before end.");
      sql += ")";
    }
    if (available)
      sql +=
          " AND s.capacity>(SELECT COUNT(*) FROM Enrollment e WHERE e.section_id=s.section_id AND"
              + " e.status='ACTIVE')";
    sql += " ORDER BY c.code,s.section_id LIMIT 50 OFFSET ?";
    args.add(page * 50);
    return s.database().queryForList(sql, args.toArray());
  }

  @GetMapping("/api/sections/{id}")
  public Object details(@PathVariable long id) {
    var row = s.section(id);
    row.put("enrolled", s.used(id));
    row.put(
        "meetings",
        s.database()
            .queryForList(
                "SELECT * FROM SectionMeeting WHERE section_id=? ORDER BY day_of_week,start_time",
                id));
    row.put(
        "prerequisites",
        s.database()
            .queryForList(
                "SELECT c.code,c.title FROM CoursePrerequisite p JOIN course c ON"
                    + " c.course_id=p.prerequisite_id WHERE p.course_id=?",
                BetaService.number(row, "course_id")));
    return row;
  }
}
