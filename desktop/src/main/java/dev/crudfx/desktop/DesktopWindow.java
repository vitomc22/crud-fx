package dev.crudfx.desktop;

import dev.crudfx.desktop.PieceGateway.Piece;
import dev.crudfx.desktop.PieceGateway.UserSession;
import java.util.Locale;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class DesktopWindow {
    private final PieceGateway gateway;
    private final BorderPane root = new BorderPane();
    private final Label statusLabel = new Label();
    private final TextField emailField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final TextField partNumberField = new TextField();
    private final TextField nameField = new TextField();
    private final TextField revisionField = new TextField();
    private final TextArea descriptionField = new TextArea();
    private final TextField searchField = new TextField();
    private final TableView<Piece> piecesTable = new TableView<>();
    private final ObservableList<Piece> pieces = FXCollections.observableArrayList();
    private final FilteredList<Piece> filteredPieces = new FilteredList<>(pieces, piece -> true);
    private final Button deleteButton = new Button("Excluir");
    private String selectedPartNumber;

    public DesktopWindow(PieceGateway gateway) {
        this.gateway = gateway;
        root.getStyleClass().add("app-root");
        var stylesheet = getClass().getResource("app.css");
        if (stylesheet != null) {
            root.getStylesheets().add(stylesheet.toExternalForm());
        }
        statusLabel.setId("statusLabel");
        statusLabel.getStyleClass().add("status-bar");
        setupTable();
        setupSearch();
        showLogin();
    }

    public Parent root() {
        return root;
    }

    private void showLogin() {
        root.setTop(null);
        root.setBottom(statusLabel);
        emailField.setId("emailField");
        emailField.setPromptText("seu@email.com");
        passwordField.setId("passwordField");
        passwordField.setPromptText("Senha");

        Label brand = new Label("CRUD FX");
        brand.getStyleClass().add("page-title");
        Label heading = new Label("Acesse o inventário");
        heading.getStyleClass().add("section-title");
        Label description = new Label("Entre para consultar e gerenciar peças.");
        description.getStyleClass().add("muted-text");

        Button loginButton = new Button("Entrar");
        loginButton.setId("loginButton");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(event -> login());
        passwordField.setOnAction(event -> login());

        VBox form = new VBox(10,
                new Label("E-mail"), emailField,
                new Label("Senha"), passwordField,
                loginButton);
        VBox panel = new VBox(16, brand, heading, description, new Separator(), form);
        panel.getStyleClass().add("login-panel");
        panel.setPadding(new Insets(28));
        VBox wrapper = new VBox(panel);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(32));
        root.setCenter(wrapper);
        setStatus("Conecte-se ao backend para abrir o inventário.", false);
    }

    private void login() {
        try {
            UserSession session = gateway.login(emailField.getText().trim(), passwordField.getText());
            showInventory(session.email());
        } catch (RuntimeException exception) {
            setStatus(messageFor(exception), true);
        }
    }

    private void showInventory(String email) {
        Label title = new Label("Inventário de peças");
        title.getStyleClass().add("page-title");
        Label account = new Label(email);
        account.setId("currentUserLabel");
        account.getStyleClass().add("muted-text");
        Button logoutButton = new Button("Sair");
        logoutButton.setId("logoutButton");
        logoutButton.getStyleClass().add("secondary");
        logoutButton.setOnAction(event -> logout());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(16, title, spacer, account, logoutButton);
        header.getStyleClass().add("page-header");

        root.setTop(header);
        root.setCenter(buildWorkspace());
        root.setBottom(statusLabel);
        clearForm();
        try {
            reloadPieces();
            setStatus(pieces.size() + " peças carregadas.", false);
        } catch (RuntimeException exception) {
            setStatus(messageFor(exception), true);
        }
    }

    private Parent buildWorkspace() {
        Label tableTitle = new Label("Peças cadastradas");
        tableTitle.getStyleClass().add("section-title");
        searchField.setId("searchField");
        searchField.setPromptText("Buscar por código, nome ou revisão");
        VBox tablePanel = new VBox(14, tableTitle, searchField, piecesTable);
        tablePanel.getStyleClass().add("table-panel");
        VBox.setVgrow(piecesTable, Priority.ALWAYS);

        Label editorTitle = new Label("Dados da peça");
        editorTitle.getStyleClass().add("section-title");
        partNumberField.setId("partNumberField");
        partNumberField.setPromptText("Ex.: BRK-042");
        nameField.setId("nameField");
        nameField.setPromptText("Nome da peça");
        revisionField.setId("revisionField");
        revisionField.setPromptText("Ex.: A");
        descriptionField.setId("descriptionField");
        descriptionField.setPromptText("Descrição opcional");
        descriptionField.setPrefRowCount(4);
        descriptionField.setWrapText(true);

        Button saveButton = new Button("Salvar peça");
        saveButton.setId("savePieceButton");
        saveButton.setMaxWidth(Double.MAX_VALUE);
        saveButton.setOnAction(event -> savePiece());
        Button newButton = new Button("Nova peça");
        newButton.setId("newPieceButton");
        newButton.getStyleClass().add("secondary");
        newButton.setMaxWidth(Double.MAX_VALUE);
        newButton.setOnAction(event -> clearForm());
        deleteButton.setId("deletePieceButton");
        deleteButton.getStyleClass().add("danger");
        deleteButton.setMaxWidth(Double.MAX_VALUE);
        deleteButton.setDisable(true);
        deleteButton.setOnAction(event -> deleteSelected());

        VBox editor = new VBox(9,
                editorTitle,
                field("Código", partNumberField),
                field("Nome", nameField),
                field("Revisão", revisionField),
                field("Descrição", descriptionField),
                newButton, saveButton, deleteButton);
        editor.getStyleClass().add("editor-panel");
        editor.setPrefWidth(340);
        ScrollPane editorScroll = new ScrollPane(editor);
        editorScroll.setFitToWidth(true);
        editorScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        editorScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        HBox workspace = new HBox(16, tablePanel, editorScroll);
        workspace.setPadding(new Insets(18));
        HBox.setHgrow(tablePanel, Priority.ALWAYS);
        return workspace;
    }

    private VBox field(String title, javafx.scene.Node control) {
        Label label = new Label(title);
        VBox box = new VBox(5, label, control);
        if (control instanceof TextField textField) {
            textField.setMaxWidth(Double.MAX_VALUE);
        } else if (control instanceof TextArea textArea) {
            textArea.setMaxWidth(Double.MAX_VALUE);
        }
        return box;
    }

    private void setupTable() {
        piecesTable.setId("piecesTable");
        piecesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<Piece, String> partNumberColumn = column("Código", Piece::partNumber);
        TableColumn<Piece, String> nameColumn = column("Nome", Piece::name);
        TableColumn<Piece, String> revisionColumn = column("Revisão", Piece::revision);
        piecesTable.getColumns().addAll(partNumberColumn, nameColumn, revisionColumn);
        piecesTable.setItems(filteredPieces);
        piecesTable.getSelectionModel().selectedItemProperty().addListener((observable, oldPiece, newPiece) -> {
            if (newPiece != null) {
                selectedPartNumber = newPiece.partNumber();
                partNumberField.setText(newPiece.partNumber());
                partNumberField.setDisable(true);
                nameField.setText(newPiece.name());
                revisionField.setText(newPiece.revision());
                descriptionField.setText(newPiece.description());
                deleteButton.setDisable(false);
            }
        });
    }

    private TableColumn<Piece, String> column(String title, Function<Piece, String> value) {
        TableColumn<Piece, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return column;
    }

    private void setupSearch() {
        searchField.textProperty().addListener((observable, oldText, newText) -> {
            String query = newText == null ? "" : newText.trim().toLowerCase(Locale.ROOT);
            filteredPieces.setPredicate(piece -> query.isEmpty()
                    || piece.partNumber().toLowerCase(Locale.ROOT).contains(query)
                    || piece.name().toLowerCase(Locale.ROOT).contains(query)
                    || piece.revision().toLowerCase(Locale.ROOT).contains(query));
        });
    }

    private void savePiece() {
        String partNumber = partNumberField.getText().trim();
        String name = nameField.getText().trim();
        String revision = revisionField.getText().trim();
        if (partNumber.isEmpty() || name.isEmpty() || revision.isEmpty()) {
            setStatus("Preencha código, nome e revisão.", true);
            return;
        }

        Piece piece = new Piece(partNumber, name, descriptionField.getText().trim(), revision);
        try {
            boolean creating = selectedPartNumber == null;
            if (creating) {
                gateway.create(piece);
            } else {
                gateway.update(piece);
            }
            reloadPieces();
            clearForm();
            setStatus(creating ? "Peça cadastrada." : "Peça atualizada.", false);
        } catch (RuntimeException exception) {
            setStatus(messageFor(exception), true);
        }
    }

    private void deleteSelected() {
        if (selectedPartNumber == null) {
            return;
        }
        try {
            gateway.delete(selectedPartNumber);
            reloadPieces();
            clearForm();
            setStatus("Peça excluída.", false);
        } catch (RuntimeException exception) {
            setStatus(messageFor(exception), true);
        }
    }

    private void reloadPieces() {
        pieces.setAll(gateway.findPieces());
    }

    private void clearForm() {
        selectedPartNumber = null;
        piecesTable.getSelectionModel().clearSelection();
        partNumberField.clear();
        partNumberField.setDisable(false);
        nameField.clear();
        revisionField.clear();
        descriptionField.clear();
        deleteButton.setDisable(true);
    }

    private void logout() {
        try {
            gateway.logout();
        } finally {
            pieces.clear();
            emailField.clear();
            passwordField.clear();
            showLogin();
        }
    }

    private void setStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().remove("status-error");
        if (error) {
            statusLabel.getStyleClass().add("status-error");
        }
    }

    private String messageFor(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "Não foi possível concluir a operação." : message;
    }
}