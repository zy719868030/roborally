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
    public void drawBoard(List<List<List<BoardElement>>> boardMap) {
        clearBoard();
        for (int x = 0; x < boardMap.size(); x++) {
            List<List<BoardElement>> col = boardMap.get(x);
            for (int y = 0; y < col.size(); y++) {
                List<BoardElement> elements = col.get(y);
                StackPane tile = createTile(elements);
                gameBoardPane.add(tile, x, y);
            }
        }
    }

    //  Erzeugt eine Zelle mit visuellen Infos
    private StackPane createTile(List<BoardElement> elements) {
        StackPane pane = new StackPane();
        Rectangle background = new Rectangle(60, 60);
        background.setStroke(Color.BLACK);
        background.setFill(Color.LIGHTGRAY);

        // Hier evtl. Icons/Symbole je nach Elementtyp hinzufügen
        Label content = new Label(elements.isEmpty() ? "" : elements.get(0).getClass().getSimpleName());
        pane.getChildren().addAll(background, content);
        return pane;
    }

    //  später verwendbar
    public void setCardSelectionVisible(boolean visible) {
        cardSelectionBox.setVisible(visible);
    }

    public void setGameBoardVisible(boolean visible) {
        gameBoardPane.setVisible(visible);
    }
}
