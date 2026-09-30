package dev.crudfx.backend.piece;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PieceRequest(
        @NotBlank @Size(max = 80) String partNumber,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 40) String revision) {
}