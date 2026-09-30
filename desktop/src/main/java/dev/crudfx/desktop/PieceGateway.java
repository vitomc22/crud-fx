package dev.crudfx.desktop;

import java.util.List;

public interface PieceGateway {
    UserSession login(String email, String password);

    List<Piece> findPieces();

    Piece create(Piece piece);

    Piece update(Piece piece);

    void delete(String partNumber);

    void logout();

    record Piece(String partNumber, String name, String description, String revision) {
    }

    record UserSession(String email) {
    }
}