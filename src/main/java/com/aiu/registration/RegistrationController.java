package com.aiu.registration;

import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
public class RegistrationController {
  private final BetaService service;

  public RegistrationController(BetaService service) {
    this.service = service;
  }

  @GetMapping("/api/requests")
  public Object requests(Authentication auth) {
    return service.requests(auth);
  }

  @PostMapping("/api/requests")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public Object submit(@RequestBody RequestData r, Authentication auth) {
    service.role(auth, "STUDENT");
    List<Long> ids =
        r.sectionIds() != null
            ? r.sectionIds()
            : r.sectionId() != null ? List.of(r.sectionId()) : List.of();
    if (ids.isEmpty()) throw new org.springframework.web.server.ResponseStatusException(
        org.springframework.http.HttpStatus.BAD_REQUEST, "Choose at least one class section.");
    ids = service.ids(ids);
    long term =
        r.termId() != null
            ? r.termId()
            : BetaService.number(service.section(ids.get(0)), "term_id");
    return service.submit(
        auth,
        term,
        ids,
        r.idempotencyKey() == null ? UUID.randomUUID().toString() : r.idempotencyKey(),
        r.planVersion());
  }

  @PatchMapping("/api/requests/{id}/status")
  public Object decide(@PathVariable long id, @RequestBody Decision d, Authentication a) {
    return service.decide(a, id, d.status(), d.reason());
  }

  @PostMapping("/api/requests/{id}/withdraw")
  public Object withdraw(@PathVariable long id, Authentication a) {
    service.withdraw(a, id);
    return Map.of("message", "Request withdrawn.");
  }

  @PostMapping("/api/advisor/registrations/validate")
  public Object advisorPreview(@RequestBody AdvisorRegistration r, Authentication a) {
    return service.advisorPreview(a, r.studentId(), r.termId(), r.sectionId());
  }

  @PostMapping("/api/advisor/registrations")
  public Object registerStudent(@RequestBody AdvisorRegistration r, Authentication a) {
    return service.registerStudent(a, r.studentId(), r.termId(), r.sectionId(), r.idempotencyKey());
  }

  public record AdvisorRegistration(long studentId, long termId, long sectionId, String idempotencyKey) {}

  @GetMapping("/api/csrf")
  public CsrfToken csrf(CsrfToken token) {
    return token;
  }

  public record RequestData(
      String studentName,
      String studentId,
      Long sectionId,
      Long termId,
      List<Long> sectionIds,
      String idempotencyKey,
      Integer planVersion) {}

  public record Decision(String status, String reason) {}
}
