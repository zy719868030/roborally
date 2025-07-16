package de.lmu.dbs.ifi.sep25.ui;

import de.lmu.dbs.ifi.sep25.network.Client;
import de.lmu.dbs.ifi.sep25.network.ClientSingleton;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.BodySendChat;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.BodySetStatus;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.Message;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class LobbyController {
    private final List<Integer> readyOrder = new ArrayList<>();
    private final ObservableList<PlayerEntry> players = FXCollections.observableArrayList();
    @FXML
    private ComboBox<PlayerEntry> recipientBox;
    @FXML
    private TextArea chatArea;
    @FXML
    private TextField chatInput;
    @FXML
    private ListView<PlayerEntry> playerList;
    @FXML
    private Button readyButton, notReadyButton;
    @FXML
    private Label statusLabel;
    @FXML
    private Label mapLabel;
    @FXML
    private VBox mapSelectionBox;
    @FXML
    private ComboBox<String> mapChoiceBox;
    @FXML
    private Button selectMapButton;
    @FXML
    private ImageView mapPreviewImage;
    @FXML
    private StackPane mapPreviewContainer;
    @FXML
    private VBox chatBox;
    @FXML
    private VBox lobbyBox;
    @FXML
    private HBox iconMenu;
    @FXML
    private Button lobbyToggleButton;
    @FXML
    private Button chatToggleButton;
    @FXML
    private Button lobbyRestoreButton;
    @FXML
    private Button chatRestoreButton;
    @FXML
    private Label lobbyIconLabel;
    @FXML
    private Label chatIconLabel;
    private List<String> lastReceivedMapList = null;
    private Parent root;

    /**
     * Wechselt die Sichtbarkeit der Lobby-Box.
     * Blendet die Lobby-Box aus oder ein, passt die zugehörigen Buttons und Icons an
     * und aktualisiert die Sichtbarkeit des Icon-Menüs.
     */
    @FXML
    private void toggleLobbyBox() {
        if (lobbyBox.isVisible()) {
            slideOut(lobbyBox);
            lobbyIconLabel.setText("▲");
            lobbyToggleButton.setVisible(false);
            lobbyToggleButton.setManaged(false);
            lobbyRestoreButton.setVisible(true);
            lobbyRestoreButton.setManaged(true);
        } else {
            slideIn(lobbyBox);
            lobbyIconLabel.setText("▼");
            lobbyToggleButton.setVisible(true);
            lobbyToggleButton.setManaged(true);
            lobbyRestoreButton.setVisible(false);
            lobbyRestoreButton.setManaged(false);
        }
        updateIconMenuVisibility();
    }

    /**
     * Wechselt die Sichtbarkeit der Chat-Box.
     * Blendet die Chat-Box aus oder ein, passt die zugehörigen Buttons und Icons an
     * und aktualisiert die Sichtbarkeit des Icon-Menüs.
     */
    @FXML
    private void toggleChatBox() {
        if (chatBox.isVisible()) {
            slideOut(chatBox);
            chatIconLabel.setText("▲");
            chatToggleButton.setVisible(false);
            chatToggleButton.setManaged(false);
            chatRestoreButton.setVisible(true);
            chatRestoreButton.setManaged(true);
        } else {
            slideIn(chatBox);
            chatIconLabel.setText("▼");
            chatToggleButton.setVisible(true);
            chatToggleButton.setManaged(true);
            chatRestoreButton.setVisible(false);
            chatRestoreButton.setManaged(false);
        }
        updateIconMenuVisibility();
    }

    /**
     * Aktualisiert die Sichtbarkeit des Icon-Menüs.
     * Zeigt das Icon-Menü an, wenn entweder die Chat-Box oder die Lobby-Box minimiert ist,
     * andernfalls wird es ausgeblendet.
     */
    private void updateIconMenuVisibility() {
        boolean irgendwasMinimiert =
                chatRestoreButton.isVisible() || lobbyRestoreButton.isVisible();

        iconMenu.setVisible(irgendwasMinimiert);
        iconMenu.setManaged(irgendwasMinimiert);


    }

    /**
     * Initialisiert die Lobby-Oberfläche.
     * Setzt die Spieler- und Empfängerlisten, konfiguriert die Anzeigeelemente,
     * richtet Listener für die Kartenwahl ein und registriert den Controller.
     * Stellt sicher, dass die UI-Elemente korrekt angezeigt und verwaltet werden.
     */
    @FXML
    public void initialize() {
        ControllerRegistry.setLobbyController(this);
        playerList.setItems(players);
        //  mapPreviewImage.fitWidthProperty().bind(mapPreviewContainer.widthProperty().subtract(20));
        //mapPreviewImage.setPreserveRatio(true);


        playerList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(PlayerEntry player, boolean empty) {
                super.updateItem(player, empty);
                if (empty || player == null) {
                    setText(null);
                    setStyle(null);
                    getStyleClass().clear();
                } else {
                    setText(player.toString());
                    getStyleClass().clear();
                    getStyleClass().add("player-cell");
                    if (player.isReady()) {
                        getStyleClass().add("player-cell-ready");
                    } else {
                        getStyleClass().add("player-cell-unready");
                    }
                }
            }
        });

        //Combobox conf
        PlayerEntry alleOption = new PlayerEntry(-1, "Alle", -1, false);
        recipientBox.getItems().add(alleOption);
        // ComboBox: Darstellung und Zellen setzen
        recipientBox.setPromptText("An...");
        recipientBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(PlayerEntry item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? "Alle" : item.getName());
            }
        });
        recipientBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PlayerEntry item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getName());
            }
        });


        recipientBox.getSelectionModel().clearSelection();
        chatInput.setOnAction(e -> handleSendChat());
        updateReadyButtons(false);
        chatBox.setVisible(true);
        chatBox.setManaged(true);
        iconMenu.setVisible(false);
        iconMenu.setManaged(false);


        // Listener für Kartenwechsel in der ComboBox
        mapChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldMap, newMap) -> {
            if (newMap != null) {
                mapLabel.setText("Ausgewählte Karte: " + newMap);
                displayMapPreview(newMap);
            }
        });

        //Add yourself to LobbyList
        Client client = ClientSingleton.getInstance();
        if (client != null) {
            client.flushPendingPlayers();
        }
        // mapPreviewImage.fitWidthProperty().bind(mapPreviewContainer.widthProperty().subtract(40));
        //mapPreviewImage.fitHeightProperty().bind(mapPreviewContainer.heightProperty().subtract(40));

        //für Buttons
        addHoverAnimation(lobbyToggleButton);
        addHoverAnimation(chatToggleButton);

    }

    /**
     * Zeigt eine Vorschau der ausgewählten Karte an.
     * Lädt das entsprechende Vorschaubild basierend auf dem Kartennamen und zeigt es an.
     * Falls kein Bild gefunden wird, wird eine Warnung ausgegeben und das Vorschaubild ausgeblendet.
     *
     * @param mapName Name der Karte, für die die Vorschau angezeigt werden soll.
     */
    private void displayMapPreview(String mapName) {
        final String imageFile = "/assets/mapPreviews/" + mapName.toLowerCase().replace(" ", "_") + ".png";
        InputStream imageStream = getClass().getResourceAsStream(imageFile);
        if (imageStream != null) {
            mapPreviewImage.setImage(new Image(imageStream));
            mapPreviewImage.setVisible(true);
            mapPreviewImage.setManaged(true);
        } else {
            System.err.println("[WARN] Kein Vorschaubild gefunden für: " + mapName);
            mapPreviewImage.setVisible(false);
            mapPreviewImage.setManaged(false);
        }
    }

    /**
     * Setzt den eigenen Status auf "bereit".
     * Sendet die Bereitschaft an den Server, aktualisiert die Anzeige und die Buttons,
     * und markiert den Spieler in der Lobby als bereit.
     */
    @FXML
    private void handleReady() {
        sendReadyStatus(true);
        statusLabel.setText("Du bist bereit.");
        updateReadyButtons(true);
        int myID = ClientSingleton.getInstance().getID();
        updatePlayerStatus(myID, true);  //Status in der PlayerList setzen

        if (!lobbyBox.getStyleClass().contains("lobby-box-ready")) {
            lobbyBox.getStyleClass().add("lobby-box-ready");
        }

    }

    /**
     * Setzt den eigenen Status auf "nicht bereit".
     * Sendet die Nicht-Bereitschaft an den Server, aktualisiert die Anzeige und die Buttons,
     * entfernt die Markierung "bereit" in der Lobby und blendet ggf. die Mapauswahl aus,
     * falls der Spieler Map-Wähler war.
     */
    @FXML
    private void handleNotReady() {
        sendReadyStatus(false);
        statusLabel.setText("Du bist nicht bereit.");
        updateReadyButtons(false);
        int myID = ClientSingleton.getInstance().getID();
        updatePlayerStatus(myID, false);  // <<< Unready setzen

        lobbyBox.getStyleClass().remove("lobby-box-ready");

        // Prüfe: Bin ich gerade der Map-Wähler?
        if (!selectMapButton.isDisabled()) {
            hideMapSelection();
        }
    }

    /**
     * Sendet den aktuellen Bereitschaftsstatus an den Server.
     * Erstellt eine Status-Nachricht und überträgt sie, sofern ein Client vorhanden ist.
     * Gibt eine Fehlermeldung aus, falls kein Client verfügbar ist.
     *
     * @param ready true, wenn der Spieler bereit ist; false, wenn nicht.
     */
    private void sendReadyStatus(boolean ready) {
        Message<BodySetStatus> msg = new Message<>(new BodySetStatus(ready));
        var client = ClientSingleton.getInstance();

        if (client != null) {
            client.sendMessage(msg);
        } else {
            System.err.println("[ERROR] ClientSingleton is null in sendReadyStatus");
        }
    }

    /**
     * Fügt einen neuen Spieler zur Lobby hinzu.
     * Erstellt einen `PlayerEntry` mit den übergebenen Parametern und fügt ihn der Spieler-Liste hinzu,
     * sofern noch kein Spieler mit dieser ID existiert. Der eigene Spielername wird mit "(du)" markiert.
     * Zusätzlich wird der Spieler der Empfänger-ComboBox hinzugefügt, falls es sich nicht um den eigenen Spieler handelt.
     *
     * @param clientID Die eindeutige ID des Spielers.
     * @param name     Der Name des Spielers.
     * @param figure   Die Figuren-ID des Spielers.
     * @param ready    Gibt an, ob der Spieler bereit ist.
     */
    public void addPlayer(int clientID, String name, int figure, boolean ready) {
        int myID = ClientSingleton.getInstance().getID();
        boolean isMe = (clientID == myID);

        String displayName = isMe ? name + " (du)" : name;

        PlayerEntry player = new PlayerEntry(clientID, displayName, figure, ready);

        // Vermeide Duplikate
        boolean alreadyExists = players.stream()
                .anyMatch(p -> p.getClientID() == clientID);
        if (!alreadyExists) {
            players.add(player);
        }

        // Vermeide Duplikate in ComboBox
        if (!isMe) {
            recipientBox.getItems().add(player);
        }

        System.out.println("[DEBUG] Spieler hinzugefügt: " + displayName);
    }

    public void showMapSelection(List<String> maps) {
        if (maps == null || maps.isEmpty()) {
            System.err.println("[WARN] showMapSelection mit leerer Map-Liste aufgerufen.");
            return;
        }

        lastReceivedMapList = maps;

        mapChoiceBox.getItems().setAll(maps);
        mapChoiceBox.getSelectionModel().selectFirst();
        mapSelectionBox.setVisible(true);
        mapSelectionBox.setManaged(true);
        selectMapButton.setDisable(false);
        mapChoiceBox.setDisable(false);
    }

    /**
     * Verarbeitet die Auswahl einer Karte durch den Spieler.
     * Sendet die ausgewählte Karte als Nachricht an den Server,
     * sofern eine gültige Auswahl getroffen wurde.
     */
    @FXML
    private void handleMapSelection() {
        String selectedMap = mapChoiceBox.getValue();
        System.out.println("[DEBUG] Karte gewählt: " + selectedMap);
        if (selectedMap != null && !selectedMap.isBlank()) {
            ClientSingleton.getInstance().sendMessage(
                    new Message<>(new MessageDefinitions.BodyMapSelected(selectedMap))
            );
        }
    }

    /**
     * Gibt die zuletzt empfangene Liste der verfügbaren Karten zurück.
     * Diese Liste wird aktualisiert, wenn neue Karten vom Server empfangen werden.
     *
     * @return Liste der zuletzt empfangenen Karten oder {@code null}, falls keine vorhanden ist.
     */
    List<String> getLastReceivedMapList() {
        return lastReceivedMapList;
    }

    /**
     * Blendet die Mapauswahl aus.
     * Setzt die Sichtbarkeit und Verwaltung der Mapauswahl-Box auf unsichtbar,
     * deaktiviert den Auswahl-Button und die Karten-ComboBox.
     * Wird z.\u200bB. aufgerufen, wenn der Spieler nicht mehr bereit ist.
     */
    public void hideMapSelection() {
        mapSelectionBox.setVisible(false);
        mapSelectionBox.setManaged(false);
        selectMapButton.setDisable(true);
        mapChoiceBox.setDisable(true);
    }

    /**
     * Setzt den Text des Karten-Labels und zeigt die zugehörige Karten-Vorschau an.
     * Extrahiert den Kartennamen aus dem übergebenen Text und lädt das entsprechende Vorschaubild.
     *
     * @param text Der anzuzeigende Text, z.\u200bB. "Ausgewählte Karte: NameDerKarte".
     */
    public void setMapLabel(String text) {
        mapLabel.setText(text);

        // Lade zugehöriges Bild
        String mapName = text.replace("Ausgewählte Karte: ", "").trim();
        displayMapPreview(mapName);
    }

    /**
     * Aktualisiert den Bereitschaftsstatus eines Spielers in der Lobby.
     * Setzt den Status des Spielers, passt die Reihenfolge der bereiten Spieler an
     * und prüft, ob der eigene Spieler nun als Erster bereit ist.
     * Falls ja, wird die Mapauswahl angezeigt, andernfalls ausgeblendet.
     * Aktualisiert die Anzeige der Spieler-Liste.
     *
     * @param clientID Die ID des Spielers, dessen Status geändert wird.
     * @param isReady  Gibt an, ob der Spieler bereit ist.
     */
    public void updatePlayerStatus(int clientID, boolean isReady) {
        PlayerEntry found = players.stream()
                .filter(p -> p.getClientID() == clientID)
                .findFirst()
                .orElse(null);

        if (found != null) {
            found.setReady(isReady);

            boolean wasFirst = !readyOrder.isEmpty() && readyOrder.get(0).equals(clientID);

            if (isReady && !readyOrder.contains(clientID)) {
                readyOrder.add(clientID);
            } else if (!isReady) {
                readyOrder.remove((Integer) clientID);
            }

            // Prüfe, ob ich jetzt der Erste bin, dann showMapSelection aktualisieren
            int myID = ClientSingleton.getInstance().getID();
            int newFirstID = readyOrder.isEmpty() ? -1 : readyOrder.get(0);

            if (myID == newFirstID) {
                // Erneut showMapSelection mit der aktuellen Liste triggern
                if (lastReceivedMapList != null) {
                    showMapSelection(lastReceivedMapList);
                }
            } else {
                hideMapSelection();
            }
            playerList.refresh();
        }
    }


    /**
     * Verarbeitet das Senden einer Chatnachricht.
     * Liest die Nachricht aus dem Eingabefeld, prüft auf leeren Inhalt und sendet sie an den ausgewählten Empfänger.
     * Die Nachricht wird an den Server übertragen und im Chatbereich angezeigt.
     * Nach dem Senden wird das Eingabefeld geleert.
     */
    @FXML
    private void handleSendChat() {
        String msg = chatInput.getText();
        if (msg == null || msg.trim().isEmpty()) return;

        PlayerEntry selected = recipientBox.getSelectionModel().getSelectedItem();
        int recipientID = (selected == null || selected.getClientID() == -1) ? -1 : selected.getClientID();

        System.out.println("[DEBUG] Sende Chatnachricht an " + (recipientID == -1 ? "ALLE" : recipientID) + ": " + msg);

        var client = ClientSingleton.getInstance();
        if (client != null) {
            client.sendMessage(new Message<>(new BodySendChat(msg, recipientID)));
        } else {
            System.err.println("[ERROR] ClientSingleton is null in handleSendChat");
        }

        String prefix = (recipientID == -1) ? "Du" : "Du → " + selected.getName();
        appendChatMessage(prefix + ": " + msg);
        chatInput.clear();
    }

    /**
     * Aktualisiert die Spieler-Liste in der Lobby.
     * Setzt die übergebene Liste als neue Spieler-Liste, entfernt alle bisherigen Einträge
     * und fügt die neuen Spieler hinzu. Der eigene Spieler wird nicht zur Empfänger-ComboBox hinzugefügt.
     * Aktualisiert die Anzeige der Spieler-Liste.
     *
     * @param newPlayers Die neue Liste der Spieler, die angezeigt werden soll.
     */
    @FXML
    public void updatePlayerList(java.util.List<PlayerEntry> newPlayers) {
        players.clear();
        recipientBox.getItems().clear();

        var client = ClientSingleton.getInstance();
        if (client == null) {
            System.err.println("[ERROR] ClientSingleton is null in addPlayer");
            return;
        }
        int myID = client.getID();

        for (PlayerEntry player : newPlayers) {
            String displayName = player.getName();
            players.add(player); // ListView

            if (player.getClientID() != myID) {
                recipientBox.getItems().add(player);
            }
        }

        playerList.refresh();
    }

    /**
     * Fügt eine Chatnachricht im Chatbereich hinzu.
     * Die übergebene Nachricht wird mit einem Zeilenumbruch an das Textfeld angehängt.
     *
     * @param message Die anzuzeigende Chatnachricht.
     */
    public void appendChatMessage(String message) {
        chatArea.appendText(message + "\n");
    }

    /**
     * Aktualisiert die Sichtbarkeit und Verwaltung der Bereitschafts-Buttons.
     * Zeigt den "Bereit"-Button an, wenn der Spieler nicht bereit ist,
     * und den "Nicht bereit"-Button, wenn der Spieler bereit ist.
     *
     * @param isReady Gibt an, ob der Spieler bereit ist.
     */
    private void updateReadyButtons(boolean isReady) {
        readyButton.setVisible(!isReady);
        readyButton.setManaged(!isReady);
        notReadyButton.setVisible(isReady);
        notReadyButton.setManaged(!isReady);
    }

    /**
     * Ändert den Namen eines Spielers in der Lobby.
     * Sucht den Spieler anhand der übergebenen ID und setzt den neuen Namen.
     * Falls es sich um den eigenen Spieler handelt, wird "(du)" angehängt.
     * Aktualisiert die Anzeige der Spieler-Liste nach der Umbenennung.
     *
     * @param clientID Die ID des Spielers, dessen Name geändert werden soll.
     * @param newName  Der neue Name, der gesetzt werden soll.
     */
    public void renamePlayer(int clientID, String newName) {
        for (PlayerEntry p : players) {
            if (p.getClientID() == clientID) {
                p.setName(newName + (clientID == ClientSingleton.getInstance().getID() ? " (du)" : ""));
                playerList.refresh();
                break;
            }

        }


    }

    /**
     * Gibt eine Liste aller Spieler in der Lobby zurück.
     * Erstellt für jeden Spieler ein neues `PlayerEntry`-Objekt, wobei der Zusatz "(du)" aus dem Namen entfernt wird.
     * Die zurückgegebene Liste enthält die aktuellen Daten aller Spieler.
     *
     * @return Liste der Spieler in der Lobby ohne Namenszusatz "(du)".
     */
    public List<PlayerEntry> getPlayers() {
        return players.stream()
                .map(p -> new PlayerEntry(
                        p.getClientID(),
                        p.getName().replace(" (du)", ""), // falls notwendig
                        p.getFigure(),
                        p.isReady()
                ))
                .toList();
    }

    /**
     * Blendet ein UI-Element mit einer Slide-Out-Animation aus.
     * Verschiebt das übergebene Node-Objekt nach unten und setzt nach Abschluss der Animation
     * die Sichtbarkeit und Verwaltung auf unsichtbar.
     *
     * @param node Das UI-Element, das ausgeblendet werden soll.
     */
    private void slideOut(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(200), node);
        tt.setToY(50);
        tt.setOnFinished(e -> {
            node.setVisible(false);
            node.setManaged(false);
        });
        tt.play();
    }

    /**
     * Blendet ein UI-Element mit einer Slide-In-Animation ein.
     * Setzt das übergebene Node-Objekt sichtbar und verwaltet,
     * und animiert es von unten nach oben in die Ausgangsposition.
     *
     * @param node Das UI-Element, das eingeblendet werden soll.
     */
    private void slideIn(Node node) {
        node.setVisible(true);
        node.setManaged(true);
        TranslateTransition tt = new TranslateTransition(Duration.millis(200), node);
        tt.setFromY(50);
        tt.setToY(0);
        tt.play();
    }

    /**
     * Gibt das aktuell gesetzte Root-Element der Lobby-Oberfläche zurück.
     *
     * @return Das gespeicherte Root-Element.
     */
    public Parent getRoot() {
        return root;
    }

    /**
     * Setzt das Root-Element der Lobby-Oberfläche.
     * Wird verwendet, um das übergeordnete Parent-Element zu speichern,
     * z.\u200bB. für spätere Zugriffe oder Anpassungen.
     *
     * @param root Das Root-Element, das gesetzt werden soll.
     */
    public void setRoot(Parent root) {
        this.root = root;
    }

    /**
     * Fügt einer Schaltfläche eine Hover-Animation hinzu.
     * Beim Überfahren mit der Maus wird die Schaltfläche leicht nach oben verschoben,
     * beim Verlassen der Maus kehrt sie in die Ausgangsposition zurück.
     *
     * @param button Die Schaltfläche, der die Animation hinzugefügt werden soll.
     */
    private void addHoverAnimation(Button button) {
        button.setOnMouseEntered(e -> {
            TranslateTransition tt = new TranslateTransition(Duration.millis(150), button);
            tt.setToY(-3);
            tt.play();
        });

        button.setOnMouseExited(e -> {
            TranslateTransition tt = new TranslateTransition(Duration.millis(150), button);
            tt.setToY(0);
            tt.play();
        });
    }
}
