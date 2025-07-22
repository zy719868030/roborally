package de.lmu.dbs.ifi.sep25.utils;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Utility for showing error dialogs in a consistent style.
 */
public class ErrorDialogUtil {

    /**
     * Shows an error alert with the given title and message, optionally running custom logic after closing.
     *
     * @param title   The title text (styled bold/red).
     * @param message The message text.
     * @param onClose Optional: code to run after the dialog closes (can be null).
     */
    public static void showError(String title, String message, Runnable onClose) {
        Platform.runLater(() -> {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initStyle(StageStyle.TRANSPARENT);

            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #b00020;");

            Label messageLabel = new Label(message);
            messageLabel.setWrapText(true);
            messageLabel.setStyle("-fx-font-size: 13px;");
            messageLabel.setMaxWidth(300);

            Button okButton = new Button("OK");
            okButton.setDefaultButton(true);
            okButton.setStyle("""
                        -fx-background-color: #e0e0e0;
                        -fx-text-fill: black;
                        -fx-font-size: 13px;
                        -fx-padding: 6 14 6 14;
                        -fx-background-radius: 6;
                        -fx-border-radius: 6;
                        -fx-cursor: hand;
                        -fx-transition: all 0.2s ease-in-out;
                    """);

            okButton.setOnMouseEntered(e -> {
                okButton.setScaleX(1.1);
                okButton.setScaleY(1.1);
            });

            okButton.setOnMouseExited(e -> {
                okButton.setScaleX(1.0);
                okButton.setScaleY(1.0);
            });

            okButton.setOnAction(_ -> {
                dialog.close();
                if (onClose != null) onClose.run();
            });

            VBox layout = new VBox(12, titleLabel, messageLabel, okButton);
            layout.setAlignment(Pos.CENTER);
            layout.setPadding(new Insets(20));
            layout.setStyle("""
                        -fx-background-color: white;
                        -fx-background-radius: 12;
                        -fx-border-radius: 12;
                        -fx-border-color: #b00020;
                        -fx-border-width: 2;
                    """);

            Scene scene = new Scene(layout);
            scene.setFill(Color.TRANSPARENT);

            scene.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ESCAPE ||
                        event.getCode() == KeyCode.SPACE ||
                        event.getCode() == KeyCode.ENTER) {
                    dialog.close();
                    if (onClose != null) onClose.run();
                }
            });

            dialog.setScene(scene);
            dialog.setResizable(false);
            dialog.show();
            scene.getRoot().requestFocus();
        });
    }

    /**
     * Overload for no custom logic (just show dialog).
     */
    public static void showError(String title, String message) {
        showError(title, message, null);
    }
}