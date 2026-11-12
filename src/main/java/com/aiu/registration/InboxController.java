package com.aiu.registration;

import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
public class InboxController {
  final BetaService s;

  public InboxController(BetaService s) {
    this.s = s;
  }

  @GetMapping("/api/waitlists")
  public Object waitlists(Authentication a) {
    s.role(a, "STUDENT");
    return s.database()
        .queryForList(
            "SELECT w.*,c.code,s.section_number FROM WaitlistEntry w JOIN section s ON"
                + " s.section_id=w.section_id JOIN course c ON c.course_id=s.course_id WHERE"
                + " w.student_id=? ORDER BY w.joined_at DESC",
            s.user(a));
  }

  @PostMapping("/api/waitlists")
  public Object join(@RequestBody Entry e, Authentication a) {
    s.joinWaitlist(a, e.sectionId());
    return Map.of("message", "Waitlist joined.");
  }

  @DeleteMapping("/api/waitlists/{id}")
  public Object leave(@PathVariable long id, Authentication a) {
    s.leaveWaitlist(a, id);
    return Map.of("message", "Waitlist left.");
  }

  @GetMapping("/api/notifications")
  public Object inbox(Authentication a) {
    return s.database()
        .queryForList(
            "SELECT notification_id,type,message,created_at,read_at FROM Notification WHERE"
                + " user_id=? ORDER BY notification_id DESC LIMIT 200",
            s.user(a));
  }

  @PatchMapping("/api/notifications/{id}/read")
  public Object read(@PathVariable long id, Authentication a) {
    if (s.database()
            .update(
                "UPDATE Notification SET read_at=CURRENT_TIMESTAMP WHERE notification_id=? AND"
                    + " user_id=?",
                id,
                s.user(a))
        != 1)
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.NOT_FOUND, "Notification not found.");
    return Map.of("message", "Marked read.");
  }

  @PostMapping("/api/notices")
  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public Object notice(@RequestBody Notice n, Authentication a) {
    s.role(a, "REGISTRAR", "ADMIN");
    s.lock();
    BetaService.require(
        n.message() != null && !n.message().isBlank() && n.message().length() <= 1000,
        "Enter a notice up to 1000 characters.");
    for (long u :
        s.database().queryForList("SELECT user_id FROM `User` WHERE active=TRUE", Long.class))
      s.notifyUser(u, "REGISTRATION_NOTICE", 0, n.message());
    s.audit(s.user(a), "PUBLISH_NOTICE", "NOTICE", 0);
    return Map.of("message", "Notice published.");
  }

  public record Entry(long sectionId) {}

  public record Notice(String message) {}
}
