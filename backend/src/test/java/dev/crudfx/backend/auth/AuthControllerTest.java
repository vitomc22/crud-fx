package dev.crudfx.backend.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/auth-test.db")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void createTestUser() {
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("DELETE FROM pieces");
        jdbcTemplate.update("INSERT INTO users (email, password_hash) VALUES (?, ?)",
                "tester@example.test", passwordEncoder.encode("correct-horse"));
    }

    @Test
    void loginOpensSessionForPieceApi() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"tester@example.test","password":"correct-horse"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        mockMvc.perform(get("/api/pieces").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void invalidLoginDoesNotOpenSession() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"tester@example.test","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/pieces"))
                .andExpect(status().isUnauthorized());
    }

        @Test
        void allowsBrowserPreflightForPieceApiWithoutSession() throws Exception {
                mockMvc.perform(options("/api/pieces")
                                                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                                                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                                                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                                .andExpect(status().isOk())
                                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
        }
}