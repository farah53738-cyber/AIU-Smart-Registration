package com.aiu.registration;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class PrerequisiteController {

  private final PrerequisiteService service;

  public PrerequisiteController(PrerequisiteService service) {
    this.service = service;
  }

  @GetMapping("/api/prerequisites/check")
  public Map<String, Object> check(@RequestParam long sectionId, Authentication authentication) {

    if (authentication.getAuthorities().stream()
        .noneMatch(a -> a.getAuthority().equals("ROLE_STUDENT")))
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.FORBIDDEN, "Student account required.");

    if (sectionId <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid section.");
    }

    List<String> missing = service.findMissingPrerequisites(sectionId, authentication.getName());

    return Map.of("eligible", missing.isEmpty(), "missingPrerequisites", missing);
  }
}
