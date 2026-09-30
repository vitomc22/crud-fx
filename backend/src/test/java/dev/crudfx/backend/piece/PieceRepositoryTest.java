package dev.crudfx.backend.piece;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@JdbcTest(properties = "spring.datasource.url=jdbc:sqlite:target/piece-test.db")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PieceRepository.class)
class PieceRepositoryTest {
    @Autowired
    private PieceRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearPieces() {
        jdbcTemplate.update("DELETE FROM pieces");
    }

    @Test
    void savesAndUpdatesPieceByPartNumber() {
        repository.save(new Piece("BRK-042", "Bracket", "Support bracket", "A"));
        repository.save(new Piece("BRK-042", "Bracket Mk II", "Revised support", "B"));

        assertThat(repository.findAll()).containsExactly(
                new Piece("BRK-042", "Bracket Mk II", "Revised support", "B"));
    }

    @Test
    void deletesByPartNumber() {
        repository.save(new Piece("BRK-043", "Bracket", "", "A"));

        assertThat(repository.deleteByPartNumber("BRK-043")).isTrue();
        assertThat(repository.findByPartNumber("BRK-043")).isEmpty();
    }
}