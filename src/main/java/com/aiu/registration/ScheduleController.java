package com.aiu.registration;

import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScheduleController {

  private final ScheduleService scheduleService;

  public ScheduleController(ScheduleService scheduleService) {
    this.scheduleService = scheduleService;
  }

  @GetMapping("/api/schedule/conflicts")
  public Map<String, Object> checkConflicts(
      @RequestParam List<Long> sectionIds, Authentication authentication) {

    if (authentication.getAuthorities().stream()
        .noneMatch(a -> a.getAuthority().equals("ROLE_STUDENT")))
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.FORBIDDEN, "Student account required.");

    List<ScheduleService.Conflict> conflicts = scheduleService.checkConflicts(sectionIds);

    return Map.of("hasConflicts", !conflicts.isEmpty(), "conflicts", conflicts);
  }
}
