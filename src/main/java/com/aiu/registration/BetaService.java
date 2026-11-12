package com.aiu.registration;

import java.sql.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BetaService {
  final JdbcTemplate db;
  final ScheduleService schedules;
  final PrerequisiteService prerequisites;

  public BetaService(
      JdbcTemplate db, ScheduleService schedules, PrerequisiteService prerequisites) {
    this.db = db;
    this.schedules = schedules;
    this.prerequisites = prerequisites;
  }

  public JdbcTemplate database() {
    return db;
  }

  public ScheduleService schedules() {
    return schedules;
  }

  static void require(boolean ok, String message) {
    if (!ok) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, message);
  }

  static long number(Map<String, Object> row, String key) {
    return ((Number) row.get(key)).longValue();
  }

  public long user(Authentication auth) {
    var users =
        db.queryForList(
            "SELECT user_id FROM `User` WHERE university_id=? AND active=TRUE", auth.getName());
    if (users.size() != 1)
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account unavailable.");
    return number(users.get(0), "user_id");
  }

  public void role(Authentication auth, String... roles) {
    user(auth);
    if (auth.getAuthorities().stream()
        .noneMatch(a -> Arrays.asList(roles).contains(a.getAuthority().replace("ROLE_", ""))))
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "This action is not available for your role.");
  }

  // A single database mutex keeps this small prototype's academic writes serial.
  // It is cross-process and transactional; replace with ordered resource locks for production load.
  public void lock() {
    db.queryForList("SELECT user_id FROM `User` ORDER BY user_id LIMIT 1 FOR UPDATE");
  }

  public long insert(String sql, Object... args) {
    var keys = new GeneratedKeyHolder();
    db.update(
        c -> {
          var p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]);
          return p;
        },
        keys);
    return Objects.requireNonNull(keys.getKeys()).values().stream()
        .filter(Number.class::isInstance)
        .map(Number.class::cast)
        .findFirst()
        .orElseThrow()
        .longValue();
  }

  public Map<String, Object> section(long id) {
    var rows =
        db.queryForList(
            "SELECT s.*,c.code,c.title,c.credits,c.active AS course_active FROM section s JOIN"
                + " course c ON c.course_id=s.course_id WHERE s.section_id=?",
            id);
    require(rows.size() == 1, "Section does not exist.");
    return rows.get(0);
  }

  public boolean flag(Object value) {
    return Boolean.TRUE.equals(value) || value instanceof Number n && n.intValue() != 0;
  }

  void termOpen(long term) {
    require(
        db.queryForObject(
                "SELECT COUNT(*) FROM `Term` WHERE term_id=? AND CURRENT_TIMESTAMP BETWEEN"
                    + " registration_open AND registration_close",
                Integer.class,
                term)
            == 1,
        "Registration is closed for this semester.");
  }

  public List<Long> ids(List<Long> sections) {
    require(
        sections != null && !sections.isEmpty() && sections.size() <= 20,
        "Select between 1 and 20 sections.");
    require(sections.stream().allMatch(x -> x != null && x > 0), "Invalid section ID.");
    require(new HashSet<>(sections).size() == sections.size(), "Remove duplicate sections.");
    return sections.stream().sorted().toList();
  }

  void validate(long student, long term, List<Long> selected, boolean seats) {
    var all = ids(selected);
    termOpen(term);
    String university =
        db.queryForObject(
            "SELECT university_id FROM `User` WHERE user_id=?", String.class, student);
    var courses = new HashSet<Long>();
    for (long id : all) {
      var s = section(id);
      require(
          s.get("term_id") != null && number(s, "term_id") == term,
          "All sections must belong to the chosen semester.");
      require(
          flag(s.get("active")) && flag(s.get("course_active")),
          "A selected section or course is inactive.");
      require(courses.add(number(s, "course_id")), "Choose only one section of each course.");
      require(
          db.queryForObject(
                  "SELECT COUNT(*) FROM SectionMeeting WHERE section_id=?", Integer.class, id)
              > 0,
          "Section " + s.get("code") + " has no meeting times.");
      var missing = prerequisites.findMissingPrerequisites(id, university);
      require(missing.isEmpty(), "Missing prerequisite(s): " + String.join(", ", missing));
      require(
          db.queryForObject(
                  "SELECT COUNT(*) FROM Enrollment e JOIN section s ON s.section_id=e.section_id"
                      + " WHERE e.student_id=? AND e.status='ACTIVE' AND s.term_id=? AND"
                      + " s.course_id=?",
                  Integer.class,
                  student,
                  term,
                  number(s, "course_id"))
              == 0,
          "You are already enrolled in " + s.get("code") + ".");
      if (seats)
        require(used(id) < number(s, "capacity"), s.get("code") + " is full. Join its waitlist.");
    }
    var combined = new ArrayList<>(all);
    combined.addAll(
        db.queryForList(
            "SELECT e.section_id FROM Enrollment e JOIN section s ON s.section_id=e.section_id"
                + " WHERE e.student_id=? AND e.status='ACTIVE' AND s.term_id=?",
            Long.class,
            student,
            term));
    var conflicts = schedules.checkConflicts(combined);
    require(
        conflicts.isEmpty(),
        "Timetable conflict: "
            + conflicts.stream()
                .map(ScheduleService.Conflict::message)
                .distinct()
                .reduce((a, b) -> a + "; " + b)
                .orElse(""));
  }

  public long used(long section) {
    return db.queryForObject(
        "SELECT COUNT(*) FROM Enrollment WHERE section_id=? AND status='ACTIVE'",
        Long.class,
        section);
  }

  public List<Map<String, Object>> terms() {
    return db.queryForList(
        "SELECT *, (CURRENT_TIMESTAMP BETWEEN registration_open AND registration_close) AS is_open"
            + " FROM `Term` ORDER BY start_date DESC");
  }

  public Map<String, Object> plan(long user, long term) {
    var plans = db.queryForList("SELECT * FROM Plan WHERE student_id=? AND term_id=?", user, term);
    if (plans.isEmpty()) return Map.of("version", 0, "sectionIds", List.of());
    var p = plans.get(0);
    return Map.of(
        "version",
        p.get("version"),
        "sectionIds",
        db.queryForList(
            "SELECT section_id FROM PlanItem WHERE plan_id=? ORDER BY section_id",
            Long.class,
            number(p, "plan_id")));
  }

  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public Map<String, Object> savePlan(long user, long term, List<Long> selected, int version) {
    lock();
    require(selected != null && selected.size() <= 20, "Draft is too large.");
    require(
        db.queryForObject("SELECT COUNT(*) FROM `Term` WHERE term_id=?", Integer.class, term) == 1,
        "Choose a semester.");
    if (!selected.isEmpty()) {
      ids(selected);
      var courses = new HashSet<Long>();
      for (long id : selected) {
        var s = section(id);
        require(s.get("term_id") != null && number(s, "term_id") == term, "Wrong semester.");
        require(courses.add(number(s, "course_id")), "Choose only one section per course.");
      }
    }
    var rows = db.queryForList("SELECT * FROM Plan WHERE student_id=? AND term_id=?", user, term);
    long plan;
    if (rows.isEmpty()) {
      if (version != 0)
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Reload your changed draft.");
      plan = insert("INSERT INTO Plan(student_id,term_id,version) VALUES(?,?,0)", user, term);
    } else {
      plan = number(rows.get(0), "plan_id");
      if (number(rows.get(0), "version") != version)
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "Another tab changed this draft. Reload it.");
    }
    db.update("DELETE FROM PlanItem WHERE plan_id=?", plan);
    for (long id : selected)
      db.update("INSERT INTO PlanItem(plan_id,section_id) VALUES(?,?)", plan, id);
    db.update(
        "UPDATE Plan SET version=version+1,updated_at=CURRENT_TIMESTAMP WHERE plan_id=?", plan);
    return plan(user, term);
  }

  public Map<String, Object> preview(long user, long term, List<Long> selected) {
    var errors = new ArrayList<String>();
    try {
      validate(user, term, selected, true);
    } catch (ResponseStatusException e) {
      errors.add(e.getReason());
    }
    return Map.of(
        "valid",
        errors.isEmpty(),
        "errors",
        errors,
        "conflicts",
        schedules.checkConflicts(selected == null ? List.of() : selected));
  }

  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public Map<String, Object> submit(
      Authentication auth, long term, List<Long> selected, String key, Integer planVersion) {
    role(auth, "STUDENT");
    long student = user(auth);
    lock();
    require(key != null && key.length() <= 64 && !key.isBlank(), "A submission key is required.");
    var prior =
        db.queryForList(
            "SELECT request_id,status,term_id FROM registrationrequest WHERE student_user_id=? AND"
                + " idempotency_key=?",
            student,
            key);
    if (!prior.isEmpty()) {
      long priorId = number(prior.get(0), "request_id");
      var old =
          db.queryForList(
              "SELECT section_id FROM RequestItem WHERE request_id=? ORDER BY section_id",
              Long.class,
              priorId);
      require(
          number(prior.get(0), "term_id") == term && old.equals(ids(selected)),
          "Submission key belongs to another schedule.");
      return Map.of(
          "requestId",
          priorId,
          "status",
          prior.get(0).get("status"),
          "message",
          "Request already saved.");
    }
    if (planVersion != null) {
      var p = plan(student, term);
      if (((Number) p.get("version")).intValue() != planVersion
          || !p.get("sectionIds").equals(ids(selected)))
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "Draft changed. Save and reload before submitting.");
    }
    require(db.queryForObject(
        "SELECT COUNT(*) FROM StudentProfile sp JOIN `User` u ON u.user_id=sp.advisor_id "
        + "JOIN UserRole r ON r.user_id=u.user_id WHERE sp.user_id=? AND u.active=TRUE AND r.role='ADVISOR'",
        Integer.class, student) > 0, "An active academic advisor must be assigned before submitting.");
    validate(student, term, selected, true);
    // Preserve conflict protection for legacy single-section requests too.
    var pending =
        db.queryForList(
            "SELECT ri.section_id FROM RequestItem ri JOIN registrationrequest r ON"
                + " r.request_id=ri.request_id WHERE r.student_user_id=? AND r.term_id=? AND"
                + " r.status='PENDING'",
            Long.class,
            student,
            term);
    var combined = new ArrayList<>(selected);
    combined.addAll(pending);
    require(
        schedules.checkConflicts(combined).isEmpty(),
        "Timetable conflict with an existing request. Review My Requests.");
    if (db.queryForObject(
            "SELECT COUNT(*) FROM registrationrequest WHERE student_user_id=? AND term_id=? AND"
                + " status='PENDING'",
            Integer.class,
            student,
            term)
        > 0)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "You already have a pending request for this semester. Withdraw it before submitting a"
              + " new plan.");
    var person = db.queryForMap("SELECT name,university_id FROM `User` WHERE user_id=?", student);
    long id =
        insert(
            "INSERT INTO"
                + " registrationrequest(student_name,student_id,section_id,student_user_id,term_id,status,idempotency_key)"
                + " VALUES(?,?,?,?,?,'PENDING',?)",
            person.get("name"),
            person.get("university_id"),
            selected.get(0),
            student,
            term,
            key);
    for (long section : selected)
      db.update("INSERT INTO RequestItem(request_id,section_id) VALUES(?,?)", id, section);
    notifyUser(
        student, "SUBMITTED", id, "Your registration request has been sent to your academic advisor for review.");
    var advisors =
        db.queryForList(
            "SELECT advisor_id FROM StudentProfile WHERE user_id=? AND advisor_id IS NOT NULL",
            Long.class,
            student);
    for (long advisor : advisors)
      notifyUser(
          advisor, "ADVISOR_REQUEST", id, "A student has submitted a registration request for your review.");
    audit(student, "SUBMIT", "REQUEST", id);
    return Map.of(
        "requestId", id, "status", "PENDING", "message", "Your registration request has been submitted for advisor review.");
  }

  public void assignedStudent(Authentication auth, long student) {
    role(auth, "ADVISOR");
    if (db.queryForObject(
        "SELECT COUNT(*) FROM StudentProfile sp JOIN `User` u ON u.user_id=sp.user_id "
        + "WHERE sp.user_id=? AND sp.advisor_id=? AND u.active=TRUE "
        + "AND EXISTS(SELECT 1 FROM UserRole r WHERE r.user_id=u.user_id AND r.role='STUDENT')",
        Integer.class, student, user(auth)) != 1)
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "Choose an active student assigned to you.");
  }

  void noPendingRequest(long student, long term) {
    if (db.queryForObject(
        "SELECT COUNT(*) FROM registrationrequest WHERE student_user_id=? AND term_id=? AND status='PENDING'",
        Integer.class, student, term) > 0)
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "This student has a pending request for this term. Review that request first.");
  }

  public Map<String, Object> advisorPreview(Authentication auth, long student, long term, long section) {
    assignedStudent(auth, student);
    noPendingRequest(student, term);
    return preview(student, term, List.of(section));
  }

  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public Map<String, Object> registerStudent(
      Authentication auth, long student, long term, long section, String key) {
    role(auth, "ADVISOR");
    lock();
    assignedStudent(auth, student);
    require(key != null && !key.isBlank() && key.length() <= 64, "A registration key is required.");
    String requestKey = "advisor:" + key;
    require(requestKey.length() <= 64, "Registration key is too long.");
    var prior = db.queryForList(
        "SELECT request_id,term_id,section_id,status,decided_by FROM registrationrequest "
        + "WHERE student_user_id=? AND idempotency_key=?", student, requestKey);
    if (!prior.isEmpty()) {
      var r = prior.get(0);
      require(number(r, "term_id") == term && number(r, "section_id") == section
          && "APPROVED".equals(r.get("status")) && number(r, "decided_by") == user(auth),
          "Registration key belongs to another registration.");
      return Map.of("requestId", number(r, "request_id"), "message", "Registration already saved.");
    }
    noPendingRequest(student, term);
    validate(student, term, List.of(section), true);
    var person = db.queryForMap("SELECT name,university_id FROM `User` WHERE user_id=?", student);
    long id = insert(
        "INSERT INTO registrationrequest(student_name,student_id,section_id,student_user_id,term_id,status,idempotency_key) "
        + "VALUES(?,?,?,?,?,'PENDING',?)",
        person.get("name"), person.get("university_id"), section, student, term, requestKey);
    db.update("INSERT INTO RequestItem(request_id,section_id) VALUES(?,?)", id, section);
    // This runs inside this method's transaction, including enrollment, notification and audit.
    decide(auth, id, "APPROVED", "Registered by academic advisor.");
    audit(user(auth), "ADVISOR_REGISTER", "REQUEST", id);
    return Map.of("requestId", id, "message", "Student registered successfully.");
  }

  public List<Map<String, Object>> requests(Authentication auth) {
    role(auth, "STUDENT", "ADVISOR");
    long me = user(auth);
    boolean advisor =
        auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADVISOR"));
    var rows =
        db.queryForList(
            "SELECT r.* FROM registrationrequest r JOIN StudentProfile sp ON"
                + " sp.user_id=r.student_user_id WHERE "
                + (advisor ? "sp.advisor_id" : "r.student_user_id")
                + "=? ORDER BY r.request_id DESC",
            me);
    for (var r : rows) {
      var items =
          db.queryForList(
              "SELECT s.section_id,c.code,c.title,s.section_number,s.schedule FROM RequestItem ri"
                  + " JOIN section s ON s.section_id=ri.section_id JOIN course c ON"
                  + " c.course_id=s.course_id WHERE ri.request_id=? ORDER BY s.section_id",
              number(r, "request_id"));
      r.put("items", items);
      r.put(
          "code",
          items.stream()
              .map(i -> i.get("code").toString())
              .reduce((a, b) -> a + ", " + b)
              .orElse(""));
      r.put("title", "Semester registration");
      r.put("section_number", "See plan");
      r.put(
          "schedule",
          items.stream()
              .map(i -> i.get("code") + ": " + i.get("schedule"))
              .reduce((a, b) -> a + " | " + b)
              .orElse(""));
    }
    return rows;
  }

  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public Map<String, String> decide(Authentication auth, long id, String status, String reason) {
    role(auth, "ADVISOR");
    long actor = user(auth);
    lock();
    require(
        Set.of("APPROVED", "REJECTED").contains(status == null ? "" : status),
        "Choose Approve or Reject.");
    var rows =
        db.queryForList(
            "SELECT r.* FROM registrationrequest r JOIN StudentProfile sp ON"
                + " sp.user_id=r.student_user_id WHERE r.request_id=? AND sp.advisor_id=?",
            id,
            actor);
    if (rows.isEmpty())
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "This request is not assigned to you.");
    var r = rows.get(0);
    if (!"PENDING".equals(r.get("status")))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Request already decided. Refresh the list.");
    long student = number(r, "student_user_id");
    require(reason == null || reason.length() <= 1000, "Reason is too long.");
    if (status.equals("REJECTED"))
      require(reason != null && !reason.isBlank(), "Enter a rejection reason.");
    else {
      require(
          db.queryForObject(
                  "SELECT COUNT(*) FROM `User` WHERE user_id=? AND active=TRUE",
                  Integer.class,
                  student)
              == 1,
          "Student account is disabled.");
      var selected =
          db.queryForList(
              "SELECT section_id FROM RequestItem WHERE request_id=? ORDER BY section_id",
              Long.class,
              id);
      validate(student, number(r, "term_id"), selected, true);
      for (long section : selected) {
        if (db.update(
                "UPDATE Enrollment SET status='ACTIVE',request_id=?,created_at=CURRENT_TIMESTAMP"
                    + " WHERE student_id=? AND section_id=? AND status='CANCELLED'",
                id,
                student,
                section)
            == 0)
          db.update(
              "INSERT INTO Enrollment(student_id,section_id,request_id,status)"
                  + " VALUES(?,?,?,'ACTIVE')",
              student,
              section,
              id);
        db.update(
            "UPDATE WaitlistEntry SET status='FULFILLED' WHERE student_id=? AND section_id=? AND"
                + " status='ACTIVE'",
            student,
            section);
      }
    }
    db.update(
        "UPDATE registrationrequest SET"
            + " status=?,reason=?,decided_at=CURRENT_TIMESTAMP,decided_by=?,version=version+1 WHERE"
            + " request_id=?",
        status,
        reason,
        actor,
        id);
    notifyUser(
        student,
        "DECISION",
        id,
        "Your registration request has been " + status.toLowerCase() + "."
            + (reason == null || reason.isBlank() ? "" : " Reason: " + reason));
    audit(actor, status, "REQUEST", id);
    return Map.of(
        "message",
        status.equals("APPROVED") ? "Registration approved. Your classes have been added to your enrollments." : "Registration request rejected. Review the reason before submitting a new plan.",
        "status",
        status);
  }

  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public void withdraw(Authentication auth, long id) {
    role(auth, "STUDENT");
    long actor = user(auth);
    lock();
    if (db.update(
            "UPDATE registrationrequest SET status='WITHDRAWN',version=version+1 WHERE request_id=?"
                + " AND student_user_id=? AND status='PENDING'",
            id,
            actor)
        != 1)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Only your pending request can be withdrawn.");
    audit(actor, "WITHDRAW", "REQUEST", id);
  }

  public List<Map<String, Object>> enrollments(long student) {
    return db.queryForList(
        "SELECT e.enrollment_id,e.section_id,s.section_number,c.code,c.title,c.credits,s.schedule,s.room,s.term_id"
            + " FROM Enrollment e JOIN section s ON s.section_id=e.section_id JOIN course c ON"
            + " c.course_id=s.course_id WHERE e.student_id=? AND e.status='ACTIVE' ORDER BY c.code",
        student);
  }

  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public void joinWaitlist(Authentication auth, long section) {
    role(auth, "STUDENT");
    long student = user(auth);
    lock();
    var s = section(section);
    require(s.get("term_id") != null, "Section has no semester.");
    termOpen(number(s, "term_id"));
    require(flag(s.get("active")) && flag(s.get("course_active")), "Section is inactive.");
    require(
        used(section) >= number(s, "capacity"),
        "Seats are available. Add this section to your plan.");
    require(
        prerequisites.findMissingPrerequisites(section, auth.getName()).isEmpty(),
        "Prerequisites are not satisfied.");
    require(
        db.queryForObject(
                "SELECT COUNT(*) FROM Enrollment e JOIN section s ON s.section_id=e.section_id"
                    + " WHERE e.student_id=? AND e.status='ACTIVE' AND s.course_id=? AND"
                    + " s.term_id=?",
                Integer.class,
                student,
                number(s, "course_id"),
                number(s, "term_id"))
            == 0,
        "Already enrolled in this course.");
    var old =
        db.queryForList(
            "SELECT * FROM WaitlistEntry WHERE student_id=? AND section_id=?", student, section);
    if (!old.isEmpty() && "ACTIVE".equals(old.get(0).get("status"))) return;
    long id;
    if (old.isEmpty())
      id =
          insert(
              "INSERT INTO WaitlistEntry(student_id,section_id,status) VALUES(?,?,'ACTIVE')",
              student,
              section);
    else {
      id = number(old.get(0), "waitlist_id");
      db.update(
          "UPDATE WaitlistEntry SET status='ACTIVE',joined_at=CURRENT_TIMESTAMP WHERE"
              + " waitlist_id=?",
          id);
    }
    notifyUser(
        student,
        "WAITLIST",
        id,
        "You joined the waitlist for "
            + s.get("code")
            + ". A notification does not reserve a seat.");
    audit(student, "JOIN_WAITLIST", "WAITLIST", id);
  }

  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public void leaveWaitlist(Authentication auth, long id) {
    role(auth, "STUDENT");
    lock();
    if (db.update(
            "UPDATE WaitlistEntry SET status='LEFT' WHERE waitlist_id=? AND student_id=? AND"
                + " status='ACTIVE'",
            id,
            user(auth))
        != 1)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Active waitlist entry not found.");
  }

  public void notifyUser(long user, String type, long aggregate, String message) {
    // Persist outbox + inbox in the same transaction. No external delivery worker is required.
    long event =
        insert(
            "INSERT INTO OutboxEvent(type,aggregate_id,payload) VALUES(?,?,?)",
            type,
            aggregate,
            "{}");
    db.update(
        "INSERT INTO Notification(user_id,event_id,type,message) VALUES(?,?,?,?)",
        user,
        event,
        type,
        message);
    db.update("UPDATE OutboxEvent SET processed_at=CURRENT_TIMESTAMP WHERE event_id=?", event);
  }

  public void seatNotice(long section) {
    if (used(section) >= number(section(section), "capacity")) return;
    for (long student :
        db.queryForList(
            "SELECT student_id FROM WaitlistEntry WHERE section_id=? AND status='ACTIVE'",
            Long.class,
            section))
      notifyUser(
          student,
          "SEAT_AVAILABLE",
          section,
          "A seat is available for "
              + section(section).get("code")
              + ". Submit a plan; seats are rechecked on approval.");
  }

  public void audit(long actor, String action, String entity, long id) {
    db.update(
        "INSERT INTO AuditLog(actor_id,action,entity_type,entity_id,request_id) VALUES(?,?,?,?,?)",
        actor,
        action,
        entity,
        id,
        UUID.randomUUID().toString());
  }

  public Map<String, Object> readiness(Authentication auth, long term) {
    role(auth, "STUDENT");
    long student = user(auth);
    require(
        db.queryForObject("SELECT COUNT(*) FROM `Term` WHERE term_id=?", Integer.class, term) == 1,
        "Choose a configured semester.");
    boolean open =
        db.queryForObject(
                "SELECT COUNT(*) FROM `Term` WHERE term_id=? AND CURRENT_TIMESTAMP BETWEEN"
                    + " registration_open AND registration_close",
                Integer.class,
                term)
            == 1;
    boolean advisor =
        db.queryForObject(
                "SELECT COUNT(*) FROM StudentProfile sp JOIN `User` u ON u.user_id=sp.advisor_id"
                    + " JOIN UserRole r ON r.user_id=u.user_id WHERE sp.user_id=? AND u.active=TRUE"
                    + " AND r.role='ADVISOR'",
                Integer.class,
                student)
            > 0;
    int pending =
        db.queryForObject(
            "SELECT COUNT(*) FROM registrationrequest WHERE student_user_id=? AND term_id=? AND"
                + " status='PENDING'",
            Integer.class,
            student,
            term);
    var draft = plan(student, term);
    var selected =
        db.queryForList(
            "SELECT pi.section_id FROM PlanItem pi JOIN Plan p ON p.plan_id=pi.plan_id WHERE"
                + " p.student_id=? AND p.term_id=? ORDER BY pi.section_id",
            Long.class,
            student,
            term);
    var preview = preview(student, term, selected);
    var errors = new ArrayList<String>();
    for (Object error : (List<?>) preview.get("errors")) errors.add(error.toString());
    if (!advisor) errors.add("An active academic advisor must be assigned. Contact the registrar.");
    if (pending > 0)
      errors.add(
          "You have a pending request for this semester. Review or withdraw it before submitting"
              + " another plan.");
    var checks = new ArrayList<Map<String, Object>>();
    for (long id : selected) {
      var section = section(id);
      var missing = prerequisites.findMissingPrerequisites(id, auth.getName());
      checks.add(
          Map.of(
              "sectionId",
              id,
              "code",
              section.get("code"),
              "schedule",
              section.get("schedule"),
              "missingPrerequisites",
              missing,
              "availableSeats",
              Math.max(0, number(section, "capacity") - used(id)),
              "active",
              flag(section.get("active")) && flag(section.get("course_active"))));
    }
    return Map.of(
        "registrationOpen",
        open,
        "advisorAssigned",
        advisor,
        "pendingRequests",
        pending,
        "draftCount",
        selected.size(),
        "ready",
        errors.isEmpty(),
        "errors",
        errors,
        "sections",
        checks,
        "draftVersion",
        draft.get("version"));
  }
}
