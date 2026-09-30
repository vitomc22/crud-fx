package dev.crudfx.qa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.crudfx.desktop.DesktopWindow;
import dev.crudfx.desktop.PieceGateway;
import dev.crudfx.desktop.PieceGateway.Piece;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.TableViewMatchers;

@ExtendWith(ApplicationExtension.class)
class DesktopWindowTest {
    private static final String EMAIL = "qa@crudfx.local";
    private static final String PASSWORD = "qa1234";
    private static final Piece SEED_PIECE = new Piece("BR'K-042", "Bracket", "Support bracket", "A");

    private InMemoryPieceGateway gateway;

    @Start
    void start(Stage stage) {
        gateway = new InMemoryPieceGateway();
        DesktopWindow window = new DesktopWindow(gateway);
        stage.setScene(new Scene(window.root(), 1120, 740));
        stage.show();
    }

    @Test
    void rejectsInvalidLogin(FxRobot robot) {
        robot.clickOn("#emailField").write(EMAIL);
        robot.clickOn("#passwordField").write("wrong-password");
        robot.clickOn("#loginButton");

        assertEquals("E-mail ou senha inválidos.", statusText(robot));
        assertTrue(robot.lookup("#loginButton").tryQuery().isPresent());
    }

    @Test
    void logsInAndFiltersPieces(FxRobot robot) {
        login(robot);
        assertEquals(EMAIL, ((Label) robot.lookup("#currentUserLabel").query()).getText());
        assertEquals(List.of(SEED_PIECE), table(robot).getItems());

        robot.clickOn("#searchField").write("BR'K-042");
        assertEquals(List.of(SEED_PIECE), table(robot).getItems());
        robot.clickOn("#searchField").eraseText("BR'K-042".length()).write("missing");
        assertTrue(table(robot).getItems().isEmpty());
    }

    @Test
    void createsUpdatesAndDeletesPiece(FxRobot robot) {
        login(robot);
        robot.clickOn("#newPieceButton");
        robot.clickOn("#partNumberField").write("WASH-101");
        robot.clickOn("#nameField").write("Washer");
        robot.clickOn("#revisionField").write("A");
        robot.clickOn("#descriptionField").write("Flat washer");
        robot.clickOn("#savePieceButton");

        assertEquals(2, table(robot).getItems().size());
        assertEquals("Peça cadastrada.", statusText(robot));

        robot.clickOn("WASH-101");
        robot.clickOn("#nameField").eraseText("Washer".length()).write("Washer Mk II");
        robot.clickOn("#savePieceButton");
        assertEquals("Washer Mk II", gateway.pieces.get("WASH-101").name());

        robot.clickOn("WASH-101");
        robot.clickOn("#deletePieceButton");
        assertFalse(gateway.pieces.containsKey("WASH-101"));
        assertEquals(1, table(robot).getItems().size());
    }

    @Test
    void editsPieceAndDisplaysUpdatedRow(FxRobot robot) {
        login(robot);
        robot.clickOn("BR'K-042");
        robot.clickOn("#nameField").eraseText(SEED_PIECE.name().length()).write("Updated bracket");
        robot.clickOn("#savePieceButton");

        FxAssert.verifyThat("#piecesTable",
                TableViewMatchers.containsRow("BR'K-042", "Updated bracket", SEED_PIECE.revision()));
        assertEquals("Updated bracket", gateway.pieces.get(SEED_PIECE.partNumber()).name());
        assertEquals("Peça atualizada.", statusText(robot));
    }

    @Test
    void requiresTheMandatoryPieceFields(FxRobot robot) {
        login(robot);
        robot.clickOn("#newPieceButton");
        robot.clickOn("#savePieceButton");

        assertEquals("Preencha código, nome e revisão.", statusText(robot));
        assertEquals(1, gateway.pieces.size());
    }

    @Test
    void logsOutAndReturnsToLogin(FxRobot robot) {
        login(robot);
        robot.clickOn("#logoutButton");

        assertEquals(1, gateway.logoutCount);
        assertTrue(robot.lookup("#loginButton").tryQuery().isPresent());
        assertFalse(robot.lookup("#piecesTable").tryQuery().isPresent());
    }

    private void login(FxRobot robot) {
        robot.clickOn("#emailField").write(EMAIL);
        robot.clickOn("#passwordField").write(PASSWORD);
        robot.clickOn("#loginButton");
    }

    private String statusText(FxRobot robot) {
        return ((Label) robot.lookup("#statusLabel").query()).getText();
    }

    @SuppressWarnings("unchecked")
    private TableView<Piece> table(FxRobot robot) {
        return (TableView<Piece>) robot.lookup("#piecesTable").query();
    }

    private static final class InMemoryPieceGateway implements PieceGateway {
        private final Map<String, Piece> pieces = new LinkedHashMap<>();
        private int logoutCount;

        private InMemoryPieceGateway() {
            pieces.put(SEED_PIECE.partNumber(), SEED_PIECE);
        }

        @Override
        public UserSession login(String email, String password) {
            if (!EMAIL.equals(email) || !PASSWORD.equals(password)) {
                throw new IllegalArgumentException("E-mail ou senha inválidos.");
            }
            return new UserSession(email);
        }

        @Override
        public List<Piece> findPieces() {
            return List.copyOf(pieces.values());
        }

        @Override
        public Piece create(Piece piece) {
            if (pieces.putIfAbsent(piece.partNumber(), piece) != null) {
                throw new IllegalStateException("Já existe uma peça com esse código.");
            }
            return piece;
        }

        @Override
        public Piece update(Piece piece) {
            if (!pieces.containsKey(piece.partNumber())) {
                throw new IllegalStateException("Peça não encontrada.");
            }
            pieces.put(piece.partNumber(), piece);
            return piece;
        }

        @Override
        public void delete(String partNumber) {
            pieces.remove(partNumber);
        }

        @Override
        public void logout() {
            logoutCount++;
        }
    }
}