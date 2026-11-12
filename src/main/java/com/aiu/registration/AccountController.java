package com.aiu.registration;

import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class AccountController {
  final BetaService s;

  public AccountController(BetaService s) {
    this.s = s;
  }

  @GetMapping("/api/me")
  public Object me(Authentication a) {
    long id = s.user(a);
    var row =
        s.database()
            .queryForMap("SELECT user_id,university_id,name FROM `User` WHERE user_id=?", id);
    var roles =
        a.getAuthorities().stream()
            .filter(r -> r.getAuthority().startsWith("ROLE_"))
            .map(r -> r.getAuthority().replace("ROLE_", ""))
            .toList();
    row.put("username", a.getName());
    row.put("displayName", row.get("name"));
    row.put("studentId", a.getName());
    row.put("role", roles.get(0));
    row.put("roles", roles);
    return row;
  }

  @GetMapping("/api/my-dashboard")
  public Object dashboard() {
    return Map.of("dashboard", "/portal.html");
  }
}
