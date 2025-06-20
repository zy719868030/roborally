package de.lmu.dbs.ifi.sep25.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import de.lmu.dbs.ifi.sep25.card.Card;
import de.lmu.dbs.ifi.sep25.game.BoardElement.BoardElement;

import java.util.List;

public class GameController {

    @FXML private Label phaseLabel;
    @FXML private HBox cardSelectionBox;
    @FXML private GridPane gameBoardPane;

    @FXML
    private GridPane boardGrid;



    @FXML
    public void initialize() {
        phaseLabel.setText("Phase: Warten...");
    }

    public void updatePhase(String phaseText) {
        phaseLabel.setText("Phase: " + phaseText);
    }

    public void showSelectedCards(List<String> cards) {
        cardSelectionBox.getChildren().clear();
        for (String cardText : cards) {
            Label cardLabel = new Label(cardText);
            cardLabel.setStyle("-fx-border-color: black; -fx-padding: 10; -fx-background-color: lightgray;");
            cardSelectionBox.getChildren().add(cardLabel);
        }
    }


    public void clearBoard() {
        gameBoardPane.getChildren().clear();
    }

    //  Anzeige des Spielfelds


    //  später verwendbar
    public void setCardSelectionVisible(boolean visible) {
        cardSelectionBox.setVisible(visible);
    }

    public void setGameBoardVisible(boolean visible) {
        gameBoardPane.setVisible(visible);
    }
}
