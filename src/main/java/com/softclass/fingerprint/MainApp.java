package com.softclass.fingerprint;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Database.init();
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("/com/softclass/fingerprint/main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        stage.setTitle("Fingerprint Attendance System");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        System.out.println("=== STARTING MAIN APP ===");
        System.setProperty("java.library.path", "./libs");
        launch();
    }
}
