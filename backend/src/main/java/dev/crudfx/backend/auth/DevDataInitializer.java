package dev.crudfx.backend.auth;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("dev")
public class DevDataInitializer {
    @Bean
    ApplicationRunner createDemoUser(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        return args -> jdbcTemplate.update(
                "INSERT OR IGNORE INTO users (email, password_hash) VALUES (?, ?)",
                "qa@crudfx.local",
                passwordEncoder.encode("qa1234"));
    }
}