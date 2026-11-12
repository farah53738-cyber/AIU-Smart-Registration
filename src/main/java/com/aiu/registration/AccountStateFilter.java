package com.aiu.registration;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AccountStateFilter extends OncePerRequestFilter {
  final JdbcTemplate db;

  public AccountStateFilter(JdbcTemplate db) {
    this.db = db;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest q, HttpServletResponse r, FilterChain chain)
      throws ServletException, IOException {
    var a = SecurityContextHolder.getContext().getAuthentication();
    if (a != null && !(a instanceof AnonymousAuthenticationToken) && a.isAuthenticated()) {
      var roles =
          new HashSet<>(
              db.queryForList(
                  "SELECT CONCAT('ROLE_',r.role) FROM `User` u JOIN UserRole r ON"
                      + " r.user_id=u.user_id WHERE u.university_id=? AND u.active=TRUE",
                  String.class,
                  a.getName()));
      var current = new HashSet<String>();
      a.getAuthorities().stream()
          .filter(x -> x.getAuthority().startsWith("ROLE_"))
          .forEach(x -> current.add(x.getAuthority()));
      if (roles.isEmpty() || !roles.equals(current)) {
        new SecurityContextLogoutHandler().logout(q, r, a);
        if (q.getRequestURI().startsWith("/api/")) r.sendError(401);
        else r.sendRedirect("/login");
        return;
      }
    }
    chain.doFilter(q, r);
  }
}
