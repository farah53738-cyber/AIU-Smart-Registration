
package com.aiu.registration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService users(
            @Value("${app.student.password}") String studentPassword,
            @Value("${app.advisor.password}") String advisorPassword,
            PasswordEncoder encoder) {

        return new InMemoryUserDetailsManager(
            User.withUsername("student1")
                .password(encoder.encode(studentPassword))
                .roles("STUDENT")
                .build(),

            User.withUsername("student2")
                .password(encoder.encode(studentPassword))
                .roles("STUDENT")
                .build(),

            User.withUsername("advisor")
                .password(encoder.encode(advisorPassword))
                .roles("ADVISOR")
                .build()
        );
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/error").permitAll()
                .requestMatchers("/advisor.html").hasRole("ADVISOR")
                .requestMatchers(
                    "/registration.html", "/requests.html"
                ).hasRole("STUDENT")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler((request, response, authentication) -> {
                    boolean isAdvisor = authentication.getAuthorities()
                        .stream()
                        .anyMatch(role -> role.getAuthority()
                            .equals("ROLE_ADVISOR"));

                    if (isAdvisor) {
                        response.sendRedirect("/advisor.html");
                    } else {
                        response.sendRedirect("/index.html");
                    }
                })
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .build();
    }
}
