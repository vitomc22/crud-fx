package dev.crudfx.backend.piece;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PieceRepository {
    private static final RowMapper<Piece> PIECE_MAPPER = (result, rowNumber) -> new Piece(
            result.getString("part_number"),
            result.getString("name"),
            result.getString("description"),
            result.getString("revision"));

    private final JdbcTemplate jdbcTemplate;

    public PieceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Piece> findAll() {
        return jdbcTemplate.query(
                "SELECT part_number, name, description, revision FROM pieces ORDER BY part_number",
                PIECE_MAPPER);
    }

    public Optional<Piece> findByPartNumber(String partNumber) {
        return jdbcTemplate.query(
                        "SELECT part_number, name, description, revision FROM pieces WHERE part_number = ?",
                        PIECE_MAPPER,
                        partNumber)
                .stream()
                .findFirst();
    }

    public Piece save(Piece piece) {
        jdbcTemplate.update("""
                INSERT INTO pieces (part_number, name, description, revision)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(part_number) DO UPDATE SET
                    name = excluded.name,
                    description = excluded.description,
                    revision = excluded.revision
                """, piece.partNumber(), piece.name(), piece.description(), piece.revision());
        return piece;
    }

    public boolean deleteByPartNumber(String partNumber) {
        return jdbcTemplate.update("DELETE FROM pieces WHERE part_number = ?", partNumber) > 0;
    }
}