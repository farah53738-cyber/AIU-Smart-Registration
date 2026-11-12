package com.aiu.registration;

import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
public class StaffController {
  final BetaService s;
  final PasswordEncoder encoder;

  public StaffController(BetaService s, PasswordEncoder e) {
    this.s = s;
    this.encoder = e;
  }

  @GetMapping("/api/reports/{type}")
  public Object report(
      @PathVariable String type,
      @RequestParam long termId,
      @RequestParam(defaultValue = "0") int page,
      Authentication a) {
    s.role(a, "REGISTRAR", "ADMIN");
    BetaService.require(page >= 0 && page <= 10000, "Invalid page.");
    String sql =
        switch (type) {
          case "seats" ->
              "SELECT c.code,s.section_id,s.section_number,s.capacity,COUNT(e.enrollment_id) AS"
                  + " enrolled,s.capacity-COUNT(e.enrollment_id) AS available FROM section s JOIN"
                  + " course c ON c.course_id=s.course_id LEFT JOIN Enrollment e ON"
                  + " e.section_id=s.section_id AND e.status='ACTIVE' WHERE s.term_id=? GROUP BY"
                  + " c.code,s.section_id,s.section_number,s.capacity ORDER BY c.code,s.section_id";
          case "enrollments" ->
              "SELECT u.university_id,u.name,c.code,s.section_number,e.status,e.created_at FROM"
                  + " Enrollment e JOIN `User` u ON u.user_id=e.student_id JOIN section s ON"
                  + " s.section_id=e.section_id JOIN course c ON c.course_id=s.course_id WHERE"
                  + " s.term_id=? ORDER BY e.enrollment_id";
          case "requests" ->
              "SELECT r.request_id,r.student_name,r.student_id,r.status,r.requested_at,r.decided_at"
                  + " FROM registrationrequest r WHERE r.term_id=? ORDER BY r.request_id";
          case "waitlists" ->
              "SELECT w.waitlist_id,u.university_id,c.code,s.section_number,w.status,w.joined_at"
                  + " FROM WaitlistEntry w JOIN `User` u ON u.user_id=w.student_id JOIN section s"
                  + " ON s.section_id=w.section_id JOIN course c ON c.course_id=s.course_id WHERE"
                  + " s.term_id=? ORDER BY w.waitlist_id";
          default ->
              throw new org.springframework.web.server.ResponseStatusException(
                  org.springframework.http.HttpStatus.BAD_REQUEST,
                  "Choose seats, enrollments, requests or waitlists.");
        };
    BetaService.require(
        s.database()
                .queryForObject(
                    "SELECT COUNT(*) FROM `Term` WHERE term_id=?", Integer.class, termId)
            == 1,
        "Semester not found.");
    return s.database().queryForList(sql + " LIMIT 100 OFFSET ?", termId, page * 100);
  }

  @GetMapping("/api/rosters")
  public Object roster(Authentication a) {
    s.role(a, "INSTRUCTOR", "REGISTRAR", "ADMIN");
    boolean staff =
        a.getAuthorities().stream()
            .anyMatch(r -> Set.of("ROLE_REGISTRAR", "ROLE_ADMIN").contains(r.getAuthority()));
    return s.database()
        .queryForList(
            "SELECT s.section_id,c.code,s.section_number,s.schedule,u.name,u.university_id FROM"
                + " section s JOIN course c ON c.course_id=s.course_id LEFT JOIN Enrollment e ON"
                + " e.section_id=s.section_id AND e.status='ACTIVE' LEFT JOIN `User` u ON"
                + " u.user_id=e.student_id "
                + (staff ? "" : "WHERE s.instructor_id=? ")
                + "ORDER BY c.code,s.section_id,u.name",
            staff ? new Object[] {} : new Object[] {s.user(a)});
  }

  @GetMapping("/api/manage/records")
  public Object records(Authentication a) {
    s.role(a, "REGISTRAR", "ADMIN");
    return Map.of(
        "Sections",
        s.database()
            .queryForList(
                "SELECT s.section_id,s.course_id,s.term_id,s.instructor_id,c.code,c.title,t.name AS term,u.name AS instructor,s.section_number,s.schedule,s.room,s.capacity,s.active"
                    + " FROM section s JOIN course c ON c.course_id=s.course_id JOIN `Term` t ON t.term_id=s.term_id LEFT JOIN `User` u ON u.user_id=s.instructor_id ORDER BY c.code,s.section_id"),
        "Prerequisites",
        s.database()
            .queryForList("SELECT cp.*,c.code AS course,p.code AS prerequisite FROM CoursePrerequisite cp JOIN course c ON c.course_id=cp.course_id JOIN course p ON p.course_id=cp.prerequisite_id ORDER BY c.code,p.code"),
        "Completions",
        s.database()
            .queryForList("SELECT cc.*,u.name AS student,c.code,t.name AS term FROM CompletedCourse cc JOIN `User` u ON u.user_id=cc.student_id JOIN course c ON c.course_id=cc.course_id JOIN `Term` t ON t.term_id=cc.term_id ORDER BY cc.student_id,cc.course_id LIMIT 200"),
        "Enrollments",
        s.database()
            .queryForList("SELECT e.*,u.name AS student,u.university_id,c.code,s.section_number,t.name AS term FROM Enrollment e JOIN `User` u ON u.user_id=e.student_id JOIN section s ON s.section_id=e.section_id JOIN course c ON c.course_id=s.course_id JOIN `Term` t ON t.term_id=s.term_id ORDER BY e.enrollment_id DESC LIMIT 200"));
  }

  @GetMapping("/api/manage/courses")
  public Object courses(Authentication a) {
    s.role(a, "REGISTRAR", "ADMIN");
    return s.database().queryForList("SELECT * FROM course ORDER BY code");
  }

  @PostMapping("/api/manage/courses")
  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public Object course(@RequestBody Course c, Authentication a) {
    s.role(a, "REGISTRAR", "ADMIN");
    s.lock();
    BetaService.require(
        c.code() != null
            && c.code().matches("[A-Za-z0-9-]{2,20}")
            && c.title() != null
            && !c.title().isBlank()
            && c.title().length() <= 100
            && c.credits() > 0
            && c.credits() <= 10,
        "Enter code, title and credits (1–10).");
    long id;
    if (c.id() == null)
      id =
          s.insert(
              "INSERT INTO course(code,title,credits,active) VALUES(?,?,?,?)",
              c.code().toUpperCase(),
              c.title(),
              c.credits(),
              c.active());
    else {
      id = c.id();
      BetaService.require(
          s.database()
                  .update(
                      "UPDATE course SET code=?,title=?,credits=?,active=? WHERE course_id=?",
                      c.code().toUpperCase(),
                      c.title(),
                      c.credits(),
                      c.active(),
                      id)
              == 1,
          "Course not found.");
    }
    s.audit(s.user(a), "SAVE_COURSE", "COURSE", id);
    return Map.of("id", id, "message", "Course saved.");
  }

  @PostMapping("/api/manage/sections")
  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public Object section(@RequestBody Section input, Authentication a) {
    s.role(a, "REGISTRAR", "ADMIN");
    s.lock();
    BetaService.require(
        input.capacity() >= 0 && input.capacity() <= 1000, "Capacity must be 0–1000.");
    BetaService.require(
        input.sectionNumber() != null
            && !input.sectionNumber().isBlank()
            && input.sectionNumber().length() <= 10
            && input.room() != null
            && !input.room().isBlank()
            && input.room().length() <= 40,
        "Section number and room are required.");
    BetaService.require(
        s.database()
                .queryForObject(
                    "SELECT COUNT(*) FROM course WHERE course_id=?",
                    Integer.class,
                    input.courseId())
            == 1,
        "Course not found.");
    BetaService.require(
        s.database()
                .queryForObject(
                    "SELECT COUNT(*) FROM `Term` WHERE term_id=?", Integer.class, input.termId())
            == 1,
        "Semester not found.");
    var teacher =
        s.database()
            .queryForList(
                "SELECT u.name FROM `User` u JOIN UserRole r ON r.user_id=u.user_id WHERE"
                    + " u.user_id=? AND u.active=TRUE AND r.role='INSTRUCTOR'",
                input.instructorId());
    BetaService.require(teacher.size() == 1, "Choose an active instructor.");
    BetaService.require(
        input.meetings() != null && !input.meetings().isEmpty() && input.meetings().size() <= 10,
        "Provide meeting times.");
    var term =
        s.database()
            .queryForMap("SELECT start_date,end_date FROM `Term` WHERE term_id=?", input.termId());
    for (var m : input.meetings()) {
      BetaService.require(
          m.day() >= 1
              && m.day() <= 7
              && m.start() != null
              && m.end() != null
              && m.end().isAfter(m.start()),
          "Invalid meeting day or time.");
      BetaService.require(
          m.startDate() != null && m.endDate() != null && !m.endDate().isBefore(m.startDate()),
          "Invalid meeting dates.");
      BetaService.require(
          !m.startDate().isBefore(((java.sql.Date) term.get("start_date")).toLocalDate())
              && !m.endDate().isAfter(((java.sql.Date) term.get("end_date")).toLocalDate()),
          "Meetings must fall within the semester.");
    }
    long id;
    long oldCapacity = 0;
    String schedule =
        input.meetings().stream()
            .map(
                m ->
                    java.time.DayOfWeek.of(m.day()).toString().substring(0, 3)
                        + " "
                        + m.start()
                        + "–"
                        + m.end())
            .reduce((x, y) -> x + ", " + y)
            .orElse("");
    if (input.id() == null)
      id =
          s.insert(
              "INSERT INTO"
                  + " section(course_id,term_id,instructor_id,section_number,instructor,schedule,room,capacity,active)"
                  + " VALUES(?,?,?,?,?,?,?,?,?)",
              input.courseId(),
              input.termId(),
              input.instructorId(),
              input.sectionNumber(),
              teacher.get(0).get("name"),
              schedule,
              input.room(),
              input.capacity(),
              input.active());
    else {
      id = input.id();
      var old = s.section(id);
      oldCapacity = BetaService.number(old, "capacity");
      BetaService.require(
          input.capacity() >= s.used(id), "Capacity cannot be below active enrollment.");
      BetaService.require(
          (BetaService.number(old, "course_id") == input.courseId()
                  && BetaService.number(old, "term_id") == input.termId())
              || s.used(id) == 0,
          "Cannot change course or semester with active enrollments.");
      s.database()
          .update(
              "UPDATE section SET"
                  + " course_id=?,term_id=?,instructor_id=?,section_number=?,instructor=?,schedule=?,room=?,capacity=?,active=?"
                  + " WHERE section_id=?",
              input.courseId(),
              input.termId(),
              input.instructorId(),
              input.sectionNumber(),
              teacher.get(0).get("name"),
              schedule,
              input.room(),
              input.capacity(),
              input.active(),
              id);
      s.database().update("DELETE FROM SectionMeeting WHERE section_id=?", id);
    }
    for (var m : input.meetings())
      s.database()
          .update(
              "INSERT INTO"
                  + " SectionMeeting(section_id,day_of_week,start_time,end_time,start_date,end_date,room)"
                  + " VALUES(?,?,?,?,?,?,?)",
              id,
              m.day(),
              java.sql.Time.valueOf(m.start()),
              java.sql.Time.valueOf(m.end()),
              java.sql.Date.valueOf(m.startDate()),
              java.sql.Date.valueOf(m.endDate()),
              input.room());
    if (input.capacity() > oldCapacity) s.seatNotice(id);
    s.audit(s.user(a), "SAVE_SECTION", "SECTION", id);
    return Map.of("id", id, "message", "Section saved.");
  }

  @PostMapping("/api/manage/terms")
  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public Object term(@RequestBody Term t, Authentication a) {
    s.role(a, "REGISTRAR");
    s.lock();
    BetaService.require(
        t.name() != null
            && !t.name().isBlank()
            && t.name().length() <= 50
            && t.start() != null
            && t.end() != null
            && t.end().isAfter(t.start())
            && t.open() != null
            && t.close() != null
            && t.close().isAfter(t.open()),
        "Provide a name and valid semester and registration dates.");
    long id;
    if (t.id() == null)
      id =
          s.insert(
              "INSERT INTO `Term`(name,start_date,end_date,registration_open,registration_close)"
                  + " VALUES(?,?,?,?,?)",
              t.name(),
              t.start(),
              t.end(),
              t.open(),
              t.close());
    else {
      id = t.id();
      BetaService.require(
          s.database()
                  .queryForObject(
                      "SELECT COUNT(*) FROM SectionMeeting m JOIN section s ON"
                          + " s.section_id=m.section_id WHERE s.term_id=? AND (m.start_date<? OR"
                          + " m.end_date>?)",
                      Integer.class,
                      id,
                      t.start(),
                      t.end())
              == 0,
          "Existing meetings fall outside these dates.");
      BetaService.require(
          s.database()
                  .update(
                      "UPDATE `Term` SET"
                          + " name=?,start_date=?,end_date=?,registration_open=?,registration_close=?"
                          + " WHERE term_id=?",
                      t.name(),
                      t.start(),
                      t.end(),
                      t.open(),
                      t.close(),
                      id)
              == 1,
          "Semester not found.");
    }
    s.audit(s.user(a), "SAVE_TERM", "TERM", id);
    return Map.of("message", "Semester saved.", "id", id);
  }

  @PostMapping("/api/manage/completions")
  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public Object completion(@RequestBody Completion c, Authentication a) {
    s.role(a, "REGISTRAR");
    s.lock();
    BetaService.require(s.database().queryForObject(
        "SELECT COUNT(*) FROM StudentProfile WHERE user_id=?", Integer.class, c.studentId()) == 1,
        "Choose a valid student.");
    BetaService.require(c.grade() == null || c.grade().length() <= 10, "Grade label is too long.");
    if (s.database()
            .update(
                "UPDATE CompletedCourse SET passed=?,grade_label=? WHERE student_id=? AND"
                    + " course_id=? AND term_id=?",
                c.passed(),
                c.grade(),
                c.studentId(),
                c.courseId(),
                c.termId())
        == 0)
      s.database()
          .update(
              "INSERT INTO CompletedCourse(student_id,course_id,term_id,passed,grade_label)"
                  + " VALUES(?,?,?,?,?)",
              c.studentId(),
              c.courseId(),
              c.termId(),
              c.passed(),
              c.grade());
    s.audit(s.user(a), "SAVE_COMPLETION", "STUDENT", c.studentId());
    return Map.of("message", "Academic record saved.");
  }

  @PostMapping("/api/manage/prerequisites")
  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public Object prerequisite(@RequestBody Prerequisite p, Authentication a) {
    s.role(a, "REGISTRAR");
    s.lock();
    BetaService.require(p.courseId() != p.prerequisiteId(), "A course cannot require itself.");
    if (p.remove())
      s.database()
          .update(
              "DELETE FROM CoursePrerequisite WHERE course_id=? AND prerequisite_id=?",
              p.courseId(),
              p.prerequisiteId());
    else {
      var seen = new HashSet<Long>();
      var todo = new ArrayDeque<Long>();
      todo.add(p.prerequisiteId());
      while (!todo.isEmpty()) {
        long id = todo.remove();
        BetaService.require(id != p.courseId(), "Prerequisite would create a cycle.");
        if (seen.add(id))
          todo.addAll(
              s.database()
                  .queryForList(
                      "SELECT prerequisite_id FROM CoursePrerequisite WHERE course_id=?",
                      Long.class,
                      id));
      }
      if (s.database()
              .queryForObject(
                  "SELECT COUNT(*) FROM CoursePrerequisite WHERE course_id=? AND prerequisite_id=?",
                  Integer.class,
                  p.courseId(),
                  p.prerequisiteId())
          == 0)
        s.database()
            .update(
                "INSERT INTO CoursePrerequisite(course_id,prerequisite_id) VALUES(?,?)",
                p.courseId(),
                p.prerequisiteId());
    }
    s.audit(s.user(a), "SAVE_PREREQUISITE", "COURSE", p.courseId());
    return Map.of("message", "Prerequisite saved.");
  }

  @PostMapping("/api/manage/enrollments/{id}/cancel")
  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public Object cancel(@PathVariable long id, Authentication a) {
    s.role(a, "REGISTRAR");
    s.lock();
    var rows =
        s.database()
            .queryForList("SELECT * FROM Enrollment WHERE enrollment_id=? AND status='ACTIVE'", id);
    BetaService.require(rows.size() == 1, "Active enrollment not found.");
    var e = rows.get(0);
    s.database().update("UPDATE Enrollment SET status='CANCELLED' WHERE enrollment_id=?", id);
    s.notifyUser(
        BetaService.number(e, "student_id"),
        "ENROLLMENT",
        id,
        "An enrollment was cancelled by the registrar.");
    s.seatNotice(BetaService.number(e, "section_id"));
    s.audit(s.user(a), "CANCEL_ENROLLMENT", "ENROLLMENT", id);
    return Map.of("message", "Enrollment cancelled; waitlist members notified.");
  }

  @GetMapping("/api/manage/users")
  public Object users(Authentication a) {
    s.role(a, "ADMIN", "REGISTRAR");
    return s.database()
        .queryForList(
            "SELECT u.user_id,u.university_id,u.name,u.active,r.role,sp.advisor_id AS advisor_id"
                + " FROM `User` u JOIN UserRole r ON r.user_id=u.user_id"
                + " LEFT JOIN StudentProfile sp ON sp.user_id=u.user_id ORDER BY u.user_id,r.role");
  }

  @PostMapping("/api/manage/users")
  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public Object user(@RequestBody Person p, Authentication a) {
    s.role(a, "ADMIN");
    s.lock();
    BetaService.require(
        p.username() != null
            && p.username().matches("[A-Za-z0-9_-]{2,30}")
            && p.name() != null
            && !p.name().isBlank()
            && p.name().length() <= 100
            && Set.of("STUDENT", "ADVISOR", "INSTRUCTOR", "REGISTRAR", "ADMIN")
                .contains(p.role() == null ? "" : p.role()),
        "Enter valid user details and role.");
    BetaService.require(
        p.id() == null || p.id() != s.user(a), "Use another administrator to edit your account.");
    if (p.password() != null && !p.password().isEmpty())
      BetaService.require(
          p.password().length() >= 8 && p.password().length() <= 72,
          "Password must have 8–72 characters.");
    long id;
    if (p.id() == null) {
      BetaService.require(
          p.password() != null && !p.password().isEmpty(), "A new account needs a password.");
      id =
          s.insert(
              "INSERT INTO `User`(university_id,name,password_hash,active) VALUES(?,?,?,?)",
              p.username(),
              p.name(),
              encoder.encode(p.password()),
              p.active());
    } else {
      id = p.id();
      BetaService.require(
          s.database()
                  .queryForObject("SELECT COUNT(*) FROM `User` WHERE user_id=?", Integer.class, id)
              == 1,
          "User not found.");
      // Avoid stripping a student's role while their academic profile/history remains.
      BetaService.require(
          p.role().equals("STUDENT")
              || s.database()
                      .queryForObject(
                          "SELECT COUNT(*) FROM StudentProfile WHERE user_id=?", Integer.class, id)
                  == 0,
          "Existing student accounts must retain the student role.");
      s.database()
          .update(
              "UPDATE `User` SET university_id=?,name=?,active=? WHERE user_id=?",
              p.username(),
              p.name(),
              p.active(),
              id);
      if (p.password() != null && !p.password().isEmpty())
        s.database()
            .update(
                "UPDATE `User` SET password_hash=? WHERE user_id=?",
                encoder.encode(p.password()),
                id);
    }
    if (p.role().equals("STUDENT") && p.advisorId() != null)
      BetaService.require(
          s.database()
                  .queryForObject(
                      "SELECT COUNT(*) FROM UserRole WHERE user_id=? AND role='ADVISOR'",
                      Integer.class,
                      p.advisorId())
              == 1,
          "Choose an advisor.");
    s.database().update("DELETE FROM UserRole WHERE user_id=?", id);
    s.database().update("INSERT INTO UserRole(user_id,role) VALUES(?,?)", id, p.role());
    if (p.role().equals("STUDENT")) {
      if (s.database()
              .update("UPDATE StudentProfile SET advisor_id=? WHERE user_id=?", p.advisorId(), id)
          == 0)
        s.database()
            .update(
                "INSERT INTO StudentProfile(user_id,advisor_id) VALUES(?,?)", id, p.advisorId());
    }
    s.audit(s.user(a), "SAVE_USER", "USER", id);
    return Map.of("id", id, "message", "User saved.");
  }

  public record Course(Long id, String code, String title, int credits, boolean active) {}

  public record Meeting(
      int day,
      java.time.LocalTime start,
      java.time.LocalTime end,
      java.time.LocalDate startDate,
      java.time.LocalDate endDate) {}

  public record Section(
      Long id,
      long courseId,
      long termId,
      long instructorId,
      String sectionNumber,
      String room,
      int capacity,
      boolean active,
      List<Meeting> meetings) {}

  public record Term(
      Long id,
      String name,
      java.time.LocalDate start,
      java.time.LocalDate end,
      java.time.LocalDateTime open,
      java.time.LocalDateTime close) {}

  public record Completion(
      long studentId, long courseId, long termId, boolean passed, String grade) {}

  public record Prerequisite(long courseId, long prerequisiteId, boolean remove) {}

  public record Person(
      Long id,
      String username,
      String name,
      String role,
      String password,
      boolean active,
      Long advisorId) {}
}
