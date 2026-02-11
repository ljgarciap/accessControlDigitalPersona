package com.softclass.fingerprint;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class Toast {
    private static Stage lastToast;

    public static void show(Stage owner, String message) {
        if (lastToast != null && lastToast.isShowing()) {
            lastToast.close();
        }

        Stage toastStage = new Stage();
        lastToast = toastStage;

        toastStage.initOwner(owner);
        toastStage.setResizable(false);
        toastStage.initStyle(StageStyle.TRANSPARENT);

        Label text = new Label(message);
        text.setFont(Font.font("System", 20)); // Aumentar un poco el tamaño
        text.setTextFill(Color.WHITE);
        text.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.8); -fx-padding: 20px; -fx-background-radius: 15px; -fx-border-color: white; -fx-border-radius: 15px;");

        StackPane root = new StackPane(text);
        root.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        toastStage.setScene(scene);

        // Posicionar en el centro del owner
        toastStage.setOnShown(e -> {
            toastStage.setX(owner.getX() + owner.getWidth() / 2 - toastStage.getWidth() / 2);
            toastStage.setY(owner.getY() + owner.getHeight() / 2 - toastStage.getHeight() / 2);
        });

        toastStage.show();

        Timeline fadeInTimeline = new Timeline();
        KeyFrame fadeInKey1 = new KeyFrame(Duration.millis(2000), e -> {
            toastStage.close();
        });
        fadeInTimeline.getKeyFrames().add(fadeInKey1);
        fadeInTimeline.play();
    }
}
