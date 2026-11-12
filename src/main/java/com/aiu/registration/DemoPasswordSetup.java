package com.aiu.registration;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DemoPasswordSetup implements ApplicationRunner {
  final JdbcTemplate db;
  final PasswordEncoder encoder;
  final String student, staff;

  public DemoPasswordSetup(
      JdbcTemplate d,
      PasswordEncoder e,
      @Value("${app.student.password}") String student,
      @Value("${app.advisor.password}") String staff) {
    db = d;
    encoder = e;
    this.student = student;
    this.staff = staff;
  }

  public void run(ApplicationArguments args) {
    // Initialize only the Step-1 demo placeholders. Never reset a real password.
    for (String name :
        List.of(
            "student1",
            "student2",
            "advisor",
            "registrar",
            "admin",
            "instructor_ahmed",
            "instructor_sara",
            "instructor_omar",
            "instructor_noor",
            "instructor_ali",
            "instructor_fatima",
            "instructor_layla",
            "instructor_khalid"))
      db.update(
          "UPDATE `User` SET password_hash=? WHERE university_id=? AND"
              + " password_hash='PENDING_BCRYPT_HASH'",
          encoder.encode(name.startsWith("student") ? student : staff),
          name);
  }
}
