package com.aiu.registration;

import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class PlanningController {
  final BetaService s;

  public PlanningController(BetaService s) {
    this.s = s;
  }

  @GetMapping("/api/terms")
  public Object terms() {
    return s.terms();
  }

  @GetMapping("/api/me/plan")
  public Object plan(@RequestParam long termId, Authentication a) {
    s.role(a, "STUDENT");
    return s.plan(s.user(a), termId);
  }

  @PutMapping("/api/me/plan")
  public Object save(@RequestBody Draft d, Authentication a) {
    s.role(a, "STUDENT");
    return s.savePlan(s.user(a), d.termId(), d.sectionIds(), d.version());
  }

  @PostMapping("/api/me/plan/validate")
  public Object validate(@RequestBody Draft d, Authentication a) {
    s.role(a, "STUDENT");
    return s.preview(s.user(a), d.termId(), d.sectionIds());
  }

  @GetMapping("/api/me/enrollments")
  public Object enrolled(Authentication a) {
    s.role(a, "STUDENT");
    return s.enrollments(s.user(a));
  }

  @GetMapping("/api/me/advisor")
  public Object advisor(Authentication a) {
    s.role(a, "STUDENT");
    return s.database()
        .queryForList(
            "SELECT u.name,u.university_id FROM StudentProfile sp JOIN `User` u ON"
                + " u.user_id=sp.advisor_id WHERE sp.user_id=?",
            s.user(a));
  }

  @GetMapping("/api/advisees")
  public Object advisees(Authentication a) {
    s.role(a, "ADVISOR");
    return s.database()
        .queryForList(
            "SELECT u.user_id,u.university_id,u.name FROM StudentProfile sp JOIN `User` u ON"
                + " u.user_id=sp.user_id WHERE sp.advisor_id=?",
            s.user(a));
  }

  @GetMapping("/api/me/readiness")
  public Object readiness(@RequestParam long termId, Authentication a) {
    return s.readiness(a, termId);
  }

  public record Draft(long termId, List<Long> sectionIds, int version) {}
}
