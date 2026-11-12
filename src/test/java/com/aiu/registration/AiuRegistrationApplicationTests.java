package com.aiu.registration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql("/test-data.sql")
class AiuRegistrationApplicationTests {
  @Autowired BetaService s;
  @Autowired StaffController staff;
  @Autowired MockMvc mvc;

  Authentication a(String name, String role) {
    return new UsernamePasswordAuthenticationToken(
        name, "", List.of(new SimpleGrantedAuthority("ROLE_" + role)));
  }

  Authentication student() {
    return a("student1", "STUDENT");
  }

  Authentication advisor() {
    return a("advisor", "ADVISOR");
  }

  long submit(Authentication a, List<Long> ids, String key) {
    return ((Number) s.submit(a, 1, ids, key, null).get("requestId")).longValue();
  }

  int count(String table) {
    return s.database().queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
  }

  @Test
  void approvalEnrollsWholePlanAndNotifiesOnce() {
    long id = submit(student(), List.of(1L, 2L), "one");
    assertEquals(0, count("Enrollment"));
    assertEquals(id, submit(student(), List.of(1L, 2L), "one"));
    assertEquals(1, count("registrationrequest"));
    assertEquals(2, count("Notification"));
    s.decide(advisor(), id, "APPROVED", null);
    assertEquals(2, count("Enrollment"));
    assertEquals(3, count("Notification"));
    assertThrows(ResponseStatusException.class, () -> s.decide(advisor(), id, "APPROVED", null));
    assertEquals(2, count("Enrollment"));
  }

  @Test
  void failedApprovalRollsBackWholePlan() {
    long id = submit(student(), List.of(1L, 2L), "two");
    s.database().update("UPDATE section SET capacity=0 WHERE section_id=2");
    assertThrows(ResponseStatusException.class, () -> s.decide(advisor(), id, "APPROVED", null));
    assertEquals(0, count("Enrollment"));
    assertEquals(
        "PENDING",
        s.database()
            .queryForObject(
                "SELECT status FROM registrationrequest WHERE request_id=?", String.class, id));
    assertEquals(2, count("Notification"));
  }

  @Test
  void conflictsPrerequisitesDuplicateCourseAndClosedTermBlock() {
    assertThrows(
        ResponseStatusException.class, () -> submit(student(), List.of(1L, 3L), "conflict"));
    assertEquals(0, count("registrationrequest"));
    assertTrue(s.schedules().checkConflicts(List.of(1L, 2L)).isEmpty());
    s.database().update("INSERT INTO CoursePrerequisite VALUES(2,1)");
    assertThrows(ResponseStatusException.class, () -> submit(student(), List.of(2L), "missing"));
    s.database()
        .update(
            "INSERT INTO CompletedCourse(student_id,course_id,term_id,passed) VALUES(1,1,1,TRUE)");
    long id = submit(student(), List.of(2L), "eligible");
    s.database().update("UPDATE CompletedCourse SET passed=FALSE");
    assertThrows(ResponseStatusException.class, () -> s.decide(advisor(), id, "APPROVED", null));
    s.database().update("UPDATE `Term` SET registration_close='2020-01-02'");
    assertFalse((Boolean) s.preview(2, 1, List.of(1L)).get("valid"));
  }

  @Test
  void draftPersistsWithVersionAndAllowsVisibleConflict() {
    var p = s.savePlan(1, 1, List.of(1L, 3L), 0);
    assertEquals(1, ((Number) p.get("version")).intValue());
    assertEquals(List.of(1L, 3L), s.plan(1, 1).get("sectionIds"));
    assertFalse((Boolean) s.preview(1, 1, List.of(1L, 3L)).get("valid"));
    assertThrows(ResponseStatusException.class, () -> s.savePlan(1, 1, List.of(1L), 0));
    s.savePlan(1, 1, List.of(1L), 1);
    assertEquals(List.of(1L), s.plan(1, 1).get("sectionIds"));
  }

  @Test
  void ownershipPendingDuplicateAndRejectionReason() {
    long id = submit(student(), List.of(1L), "pending");
    assertThrows(ResponseStatusException.class, () -> submit(student(), List.of(2L), "second"));
    assertThrows(
        ResponseStatusException.class,
        () -> s.decide(a("advisor2", "ADVISOR"), id, "APPROVED", null));
    assertThrows(ResponseStatusException.class, () -> s.decide(advisor(), id, "REJECTED", ""));
    assertThrows(ResponseStatusException.class, () -> s.withdraw(a("student2", "STUDENT"), id));
    s.decide(advisor(), id, "REJECTED", "Choose a different section.");
    assertEquals(0, count("Enrollment"));
  }

  @Test
  void concurrentFinalSeatOnlyOneApproval() throws Exception {
    long first = submit(student(), List.of(4L), "seat1"),
        second = submit(a("student2", "STUDENT"), List.of(4L), "seat2");
    var executor = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    try {
      var results = new ArrayList<Future<Boolean>>();
      for (long id : List.of(first, second))
        results.add(
            executor.submit(
                () -> {
                  start.await();
                  try {
                    s.decide(advisor(), id, "APPROVED", null);
                    return true;
                  } catch (ResponseStatusException e) {
                    return false;
                  }
                }));
      start.countDown();
      int successes = 0;
      for (var result : results) if (result.get(20, TimeUnit.SECONDS)) successes++;
      assertEquals(1, successes);
      assertEquals(1, count("Enrollment"));
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void waitlistDuplicateLeaveAndSeatNotification() {
    long id = submit(student(), List.of(4L), "filled");
    s.decide(advisor(), id, "APPROVED", null);
    var other = a("student2", "STUDENT");
    s.joinWaitlist(other, 4);
    s.joinWaitlist(other, 4);
    assertEquals(1, count("WaitlistEntry"));
    long enrollment =
        s.database().queryForObject("SELECT enrollment_id FROM Enrollment", Long.class);
    staff.cancel(enrollment, a("registrar", "REGISTRAR"));
    assertEquals(
        1,
        s.database()
            .queryForObject(
                "SELECT COUNT(*) FROM Notification WHERE user_id=2 AND type='SEAT_AVAILABLE'",
                Integer.class));
    long wait = s.database().queryForObject("SELECT waitlist_id FROM WaitlistEntry", Long.class);
    s.leaveWaitlist(other, wait);
    assertEquals(
        "LEFT", s.database().queryForObject("SELECT status FROM WaitlistEntry", String.class));
  }

  @Test
  void endpointsEnforceRoleOwnershipCsrfAndDisable() throws Exception {
    mvc.perform(get("/api/requests")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/reports/seats?termId=1").with(user("student1").roles("STUDENT")))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/requests")
                .with(user("student1").roles("STUDENT"))
                .contentType("application/json")
                .content("{\"sectionId\":1}"))
        .andExpect(status().isForbidden());
    long id = submit(student(), List.of(1L), "own");
    mvc.perform(get("/api/requests").with(user("student2").roles("STUDENT")))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
    mvc.perform(
            patch("/api/requests/" + id + "/status")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"status\":\"APPROVED\"}"))
        .andExpect(status().isForbidden());
    s.database().update("UPDATE `User` SET active=FALSE WHERE user_id=1");
    mvc.perform(get("/api/me").with(user("student1").roles("STUDENT")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void notificationsBelongToRecipientAndRosterToInstructor() throws Exception {
    submit(student(), List.of(1L), "notice");
    long id =
        s.database()
            .queryForObject(
                "SELECT MIN(notification_id) FROM Notification WHERE user_id=1", Long.class);
    mvc.perform(
            patch("/api/notifications/" + id + "/read")
                .with(user("student2").roles("STUDENT"))
                .with(csrf()))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/rosters").with(user("instructor_ahmed").roles("INSTRUCTOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void catalogFiltersAndReportsUseCurrentSeats() throws Exception {
    mvc.perform(
            get("/api/sections?termId=1&q=CSC101&day=7&startsAfter=09:00&endsBefore=10:00")
                .with(user("student1").roles("STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    mvc.perform(get("/api/reports/seats?termId=1").with(user("registrar").roles("REGISTRAR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(4));
  }

  @Test
  void loginUsesDatabaseHashAndRejectsWrongPassword() throws Exception {
    var encoder =
        org.springframework.security.crypto.factory.PasswordEncoderFactories
            .createDelegatingPasswordEncoder();
    s.database()
        .update(
            "UPDATE `User` SET password_hash=? WHERE user_id=1",
            encoder.encode("DemoPassword123!"));
    mvc.perform(
            post("/login").with(csrf()).param("username", "student1").param("password", "wrong"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?error"));
    mvc.perform(
            post("/login")
                .with(csrf())
                .param("username", "student1")
                .param("password", "DemoPassword123!"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/index.html"));
  }

  @Test
  void staleDraftAndChangedIdempotencyPayloadRejected() {
    s.savePlan(1, 1, List.of(1L), 0);
    assertThrows(
        ResponseStatusException.class, () -> s.submit(student(), 1, List.of(1L), "stale", 0));
    assertEquals(0, count("registrationrequest"));
    s.submit(student(), 1, List.of(1L), "fresh", 1);
    assertThrows(
        ResponseStatusException.class, () -> s.submit(student(), 1, List.of(2L), "fresh", null));
    assertEquals(1, count("registrationrequest"));
  }

  @Test
  void prerequisiteCyclesAndCapacityBelowEnrollmentRejected() {
    var reg = a("registrar", "REGISTRAR");
    staff.prerequisite(new StaffController.Prerequisite(2, 1, false), reg);
    assertThrows(
        ResponseStatusException.class,
        () -> staff.prerequisite(new StaffController.Prerequisite(1, 2, false), reg));
    assertEquals(1, count("CoursePrerequisite"));
    long id = submit(student(), List.of(1L), "capacity");
    s.decide(advisor(), id, "APPROVED", null);
    var m =
        new StaffController.Meeting(
            7,
            java.time.LocalTime.of(9, 0),
            java.time.LocalTime.of(10, 0),
            java.time.LocalDate.of(2026, 1, 1),
            java.time.LocalDate.of(2030, 12, 31));
    assertThrows(
        ResponseStatusException.class,
        () ->
            staff.section(
                new StaffController.Section(1L, 1, 1, 6, "01", "A1", 0, true, List.of(m)), reg));
    assertEquals(
        2,
        s.database()
            .queryForObject("SELECT capacity FROM section WHERE section_id=1", Integer.class));
  }

  @Test
  void newStudentCanUseDatabaseAccountAndRoleChangeInvalidatesSession() throws Exception {
    var result =
        (Map<?, ?>)
            staff.user(
                new StaffController.Person(
                    null, "newstudent", "New Student", "STUDENT", "Password123!", true, 3L),
                a("admin", "ADMIN"));
    long id = ((Number) result.get("id")).longValue();
    assertEquals(
        3,
        s.database()
            .queryForObject(
                "SELECT advisor_id FROM StudentProfile WHERE user_id=?", Integer.class, id));
    s.savePlan(id, 1, List.of(1L), 0);
    submit(a("newstudent", "STUDENT"), List.of(1L), "newaccount");
    s.database().update("UPDATE UserRole SET role='REGISTRAR' WHERE user_id=3");
    mvc.perform(get("/api/me").with(user("advisor").roles("ADVISOR")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void nonoverlappingTeachingDatesDoNotConflict() {
    s.database().update("UPDATE SectionMeeting SET start_date='2028-01-01' WHERE section_id=3");
    s.database().update("UPDATE SectionMeeting SET end_date='2027-12-31' WHERE section_id=1");
    assertTrue(s.schedules().checkConflicts(List.of(1L, 3L)).isEmpty());
  }

  @Test
  void realLoginSessionRemainsAuthenticated() throws Exception {
    var encoder =
        org.springframework.security.crypto.factory.PasswordEncoderFactories
            .createDelegatingPasswordEncoder();
    s.database()
        .update(
            "UPDATE `User` SET password_hash=? WHERE user_id=1",
            encoder.encode("SessionPassword123!"));
    var login =
        mvc.perform(
                post("/login")
                    .with(csrf())
                    .param("username", "student1")
                    .param("password", "SessionPassword123!"))
            .andExpect(status().is3xxRedirection())
            .andReturn();
    var session =
        (org.springframework.mock.web.MockHttpSession) login.getRequest().getSession(false);
    assertNotNull(session);
    mvc.perform(get("/api/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("STUDENT"));
    mvc.perform(get("/portal.html").session(session)).andExpect(status().isOk());
  }

  @Test
  void calendarDatesDoNotShiftAcrossTimezones() throws Exception {
    mvc.perform(get("/api/sections/1").with(user("student1").roles("STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.meetings[0].start_date").value("2026-01-01"))
        .andExpect(jsonPath("$.meetings[0].end_date").value("2030-12-31"));
  }

  @Test
  void readinessChecksOnlyOwnDraftAndRequiresStudentRole() throws Exception {
    s.savePlan(1, 1, List.of(1L), 0);
    mvc.perform(get("/api/me/readiness?termId=1").with(user("student1").roles("STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ready").value(true))
        .andExpect(jsonPath("$.draftCount").value(1));
    mvc.perform(get("/api/me/readiness?termId=1").with(user("student2").roles("STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ready").value(false))
        .andExpect(jsonPath("$.draftCount").value(0));
    mvc.perform(get("/api/me/readiness?termId=1").with(user("advisor").roles("ADVISOR")))
        .andExpect(status().isForbidden());
  }

  @Test
  void readinessShowsMissingPrerequisitesAndPendingRequest() throws Exception {
    s.savePlan(1, 1, List.of(2L), 0);
    s.database().update("INSERT INTO CoursePrerequisite VALUES(2,1)");
    mvc.perform(get("/api/me/readiness?termId=1").with(user("student1").roles("STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ready").value(false))
        .andExpect(jsonPath("$.sections[0].missingPrerequisites[0]").value("CSC101"));
    s.database().update("DELETE FROM CoursePrerequisite");
    submit(student(), List.of(2L), "readiness-pending");
    mvc.perform(get("/api/me/readiness?termId=1").with(user("student1").roles("STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pendingRequests").value(1))
        .andExpect(jsonPath("$.ready").value(false));
  }

  @Test
  void futureTermDraftCanBeSavedBeforeWindowOpens() throws Exception {
    s.database().update("UPDATE `Term` SET registration_open='2030-01-01 00:00:00'");
    s.savePlan(1, 1, List.of(1L), 0);
    assertEquals(List.of(1L), s.plan(1, 1).get("sectionIds"));
    mvc.perform(get("/api/me/readiness?termId=1").with(user("student1").roles("STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.registrationOpen").value(false))
        .andExpect(jsonPath("$.ready").value(false));
    assertThrows(ResponseStatusException.class, () -> submit(student(), List.of(1L), "future"));
  }
  @Test
  void emptyRequestReturnsBadRequestInsteadOfServerError() throws Exception {
    mvc.perform(post("/api/requests")
      .with(user("student1").roles("STUDENT"))
      .with(csrf())
      .contentType("application/json")
      .content("{\"termId\":1,\"sectionIds\":[]}"))
      .andExpect(status().isBadRequest());
  }


  @Test
  void unassignedAdvisorBlocksDirectSubmission() {
    s.database().update("UPDATE StudentProfile SET advisor_id=NULL WHERE user_id=1");
    assertThrows(ResponseStatusException.class, () -> submit(student(), List.of(1L), "noadvisor"));
    assertEquals(0, count("registrationrequest"));
  }

  @Test
  void cancelledEnrollmentCanBeRequestedAgain() {
    long first = submit(student(), List.of(1L), "first");
    s.decide(advisor(), first, "APPROVED", null);
    long enrollment = s.database().queryForObject("SELECT enrollment_id FROM Enrollment", Long.class);
    staff.cancel(enrollment, a("registrar", "REGISTRAR"));
    long second = submit(student(), List.of(1L), "second");
    s.decide(advisor(), second, "APPROVED", null);
    assertEquals(1, count("Enrollment"));
    assertEquals("ACTIVE", s.database().queryForObject("SELECT status FROM Enrollment", String.class));
  }

  @Test
  void advisorRegistersAssignedStudentAndRetryDoesNotDuplicate() {
    var first = s.registerStudent(advisor(), 1, 1, 1, "direct-one");
    assertEquals(first.get("requestId"), s.registerStudent(advisor(), 1, 1, 1, "direct-one").get("requestId"));
    assertEquals(1, count("Enrollment"));
    assertEquals(1, count("Notification"));
    assertEquals("APPROVED", s.database().queryForObject("SELECT status FROM registrationrequest", String.class));
    assertEquals(3L, s.database().queryForObject("SELECT decided_by FROM registrationrequest", Long.class));
    assertThrows(ResponseStatusException.class, () -> s.registerStudent(advisor(), 1, 1, 2, "direct-one"));
    assertThrows(ResponseStatusException.class, () -> s.registerStudent(advisor(), 1, 1, 1, "new-key"));
  }

  @Test
  void advisorRegistrationRestrictsStudentRoleAssignmentAndActiveAccount() {
    assertThrows(ResponseStatusException.class, () -> s.registerStudent(student(), 1, 1, 1, "wrong-role"));
    assertThrows(ResponseStatusException.class, () -> s.registerStudent(a("advisor2", "ADVISOR"), 1, 1, 1, "unassigned"));
    assertThrows(ResponseStatusException.class, () -> s.advisorPreview(a("advisor2", "ADVISOR"), 1, 1, 1));
    s.database().update("UPDATE `User` SET active=FALSE WHERE user_id=1");
    assertThrows(ResponseStatusException.class, () -> s.registerStudent(advisor(), 1, 1, 1, "disabled"));
    assertEquals(0, count("Enrollment"));
    assertEquals(0, count("registrationrequest"));
  }

  @Test
  void advisorRegistrationUsesPrerequisitesConflictsWindowAndPendingChecks() {
    s.database().update("INSERT INTO CoursePrerequisite VALUES(2,1)");
    assertThrows(ResponseStatusException.class, () -> s.registerStudent(advisor(), 1, 1, 2, "prereq"));
    long pending=submit(student(), List.of(1L), "pending-direct");
    assertThrows(ResponseStatusException.class, () -> s.registerStudent(advisor(), 1, 1, 4, "pending"));
    s.withdraw(student(), pending);
    s.registerStudent(advisor(), 1, 1, 1, "first-class");
    assertThrows(ResponseStatusException.class, () -> s.registerStudent(advisor(), 1, 1, 3, "conflict"));
    s.database().update("UPDATE `Term` SET registration_close='2020-01-02'");
    assertThrows(ResponseStatusException.class, () -> s.registerStudent(advisor(), 1, 1, 4, "closed"));
    assertEquals(1, count("Enrollment"));
  }

  @Test
  void advisorFullClassBlocksThenReleasedSeatFulfillsWaitlist() {
    s.registerStudent(advisor(), 1, 1, 4, "fill");
    s.joinWaitlist(a("student2", "STUDENT"), 4);
    assertThrows(ResponseStatusException.class, () -> s.registerStudent(advisor(), 2, 1, 4, "full"));
    assertEquals(1, count("registrationrequest"));
    long enrollment=s.database().queryForObject("SELECT enrollment_id FROM Enrollment", Long.class);
    staff.cancel(enrollment, a("registrar", "REGISTRAR"));
    s.registerStudent(advisor(), 2, 1, 4, "free");
    assertEquals("FULFILLED", s.database().queryForObject("SELECT status FROM WaitlistEntry", String.class));
    assertEquals(1L, s.used(4));
  }

  @Test
  void advisorRegistrationEndpointRequiresCsrfAndAdvisorRole() throws Exception {
    String body="{\"studentId\":1,\"termId\":1,\"sectionId\":1,\"idempotencyKey\":\"endpoint\"}";
    mvc.perform(post("/api/advisor/registrations").with(user("advisor").roles("ADVISOR"))
        .contentType("application/json").content(body)).andExpect(status().isForbidden());
    mvc.perform(post("/api/advisor/registrations").with(user("student1").roles("STUDENT"))
        .with(csrf()).contentType("application/json").content(body)).andExpect(status().isForbidden());
    mvc.perform(post("/api/advisor/registrations").with(user("advisor").roles("ADVISOR"))
        .with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk());
    assertEquals(1, count("Enrollment"));
  }
}
