package dev.crudfx.desktop;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class CrudFxDesktopApplication extends Application {
    @Override
    public void start(Stage stage) {
        DesktopWindow window = new DesktopWindow(new HttpPieceGateway());
        stage.setTitle("CRUD FX | Peças");
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        stage.setScene(new Scene(window.root(), 1120, 740));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}