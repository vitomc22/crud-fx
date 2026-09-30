package dev.crudfx.backend.piece;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pieces")
public class PieceController {
    private final PieceRepository repository;

    public PieceController(PieceRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Piece> findAll() {
        return repository.findAll();
    }

    @GetMapping("/{partNumber}")
    public ResponseEntity<Piece> findOne(@PathVariable String partNumber) {
        return repository.findByPartNumber(partNumber)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Piece> create(@Valid @RequestBody PieceRequest request) {
        if (repository.findByPartNumber(request.partNumber()).isPresent()) {
            return ResponseEntity.status(409).build();
        }
        Piece piece = toPiece(request);
        repository.save(piece);
        return ResponseEntity.created(URI.create("/api/pieces/" + piece.partNumber())).body(piece);
    }

    @PutMapping("/{partNumber}")
    public ResponseEntity<Piece> update(
            @PathVariable String partNumber,
            @Valid @RequestBody PieceRequest request) {
        if (!partNumber.equals(request.partNumber())) {
            return ResponseEntity.badRequest().build();
        }
        if (repository.findByPartNumber(partNumber).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Piece piece = toPiece(request);
        repository.save(piece);
        return ResponseEntity.ok(piece);
    }

    @DeleteMapping("/{partNumber}")
    public ResponseEntity<Void> delete(@PathVariable String partNumber) {
        return repository.deleteByPartNumber(partNumber)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    private Piece toPiece(PieceRequest request) {
        return new Piece(request.partNumber().trim(), request.name().trim(),
                request.description() == null ? "" : request.description().trim(), request.revision().trim());
    }
}