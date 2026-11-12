package com.aiu.registration;

import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  UserDetailsService users(JdbcTemplate db) {
    return username -> {
      var rows = db.queryForList("SELECT * FROM `User` WHERE university_id=?", username);
      if (rows.size() != 1) throw new UsernameNotFoundException("Invalid account");
      var u = rows.get(0);
      var roles =
          db.queryForList(
              "SELECT role FROM UserRole WHERE user_id=?", String.class, u.get("user_id"));
      boolean active =
          Boolean.TRUE.equals(u.get("active"))
              || u.get("active") instanceof Number n && n.intValue() != 0;
      return User.withUsername(username)
          .password((String) u.get("password_hash"))
          .roles(roles.toArray(String[]::new))
          .disabled(!active)
          .build();
    };
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http, AccountStateFilter gate) throws Exception {
    return http.authorizeHttpRequests(
            a ->
                a.requestMatchers("/login", "/error")
                    .permitAll()
                    .requestMatchers("/advisor.html")
                    .hasRole("ADVISOR")
                    .requestMatchers("/registration.html", "/requests.html")
                    .hasRole("STUDENT")
                    .anyRequest()
                    .authenticated())
        .formLogin(
            f ->
                f.loginPage("/login")
                    .successHandler(
                        (q, r, a) -> {
                          boolean student =
                              a.getAuthorities().stream()
                                  .anyMatch(role -> role.getAuthority().equals("ROLE_STUDENT"));
                          r.sendRedirect(student ? "/index.html" : "/portal.html");
                        })
                    .permitAll())
        .exceptionHandling(
            e ->
                e.defaultAuthenticationEntryPointFor(
                    (q, r, ex) -> r.sendError(401), q -> q.getRequestURI().startsWith("/api/")))
        .addFilterAfter(
            gate,
            org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class)
        .logout(
            l ->
                l.logoutSuccessUrl("/login?logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll())
        .build();
  }
}
