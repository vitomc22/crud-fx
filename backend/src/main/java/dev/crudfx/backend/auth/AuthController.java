package dev.crudfx.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public AuthController(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public UserSession login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        var users = jdbcTemplate.query(
                "SELECT id, email, password_hash FROM users WHERE email = ?",
                (result, rowNumber) -> new StoredUser(
                        result.getLong("id"), result.getString("email"), result.getString("password_hash")),
                request.email().trim().toLowerCase());
        if (users.isEmpty() || !passwordEncoder.matches(request.password(), users.getFirst().passwordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos");
        }
        HttpSession session = servletRequest.getSession(true);
        servletRequest.changeSessionId();
        session.setAttribute(SessionAuthFilter.AUTHENTICATED_USER_ID, users.getFirst().id());
        session.setAttribute(SessionAuthFilter.AUTHENTICATED_EMAIL, users.getFirst().email());
        return new UserSession(users.getFirst().email());
    }

    @GetMapping("/me")
    public UserSession currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object email = session == null ? null : session.getAttribute(SessionAuthFilter.AUTHENTICATED_EMAIL);
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return new UserSession(email.toString());
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record UserSession(String email) {
    }

    private record StoredUser(long id, String email, String passwordHash) {
    }
}