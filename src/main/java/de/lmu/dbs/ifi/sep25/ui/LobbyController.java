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
     * Toggles the visibility of the lobby box.
     * Hides or shows the lobby box, adjusts the corresponding buttons and icons,
     * and updates the visibility of the icon menu.
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
     * Toggles the visibility of the chat box.
     * Hides or shows the chat box, adjusts the corresponding buttons and icons,
     * and updates the visibility of the icon menu.
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
     * Updates the visibility of the icon menu.
     * Shows the icon menu if either the chat box or the lobby box is minimized,
     * otherwise hides it.
     */
    private void updateIconMenuVisibility() {
        boolean irgendwasMinimiert =
                chatRestoreButton.isVisible() || lobbyRestoreButton.isVisible();

        iconMenu.setVisible(irgendwasMinimiert);
        iconMenu.setManaged(irgendwasMinimiert);


    }

    /**
     * Initializes the lobby UI.
     * Sets up the player and recipient lists, configures display elements,
     * sets up listeners for map selection, and registers the controller.
     * Ensures that UI elements are displayed and managed correctly.
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
     * Displays a preview of the selected map.
     * Loads the corresponding preview image based on the map name and displays it.
     * If no image is found, a warning is issued and the preview is hidden.
     *
     * @param mapName Name of the map for which the preview should be shown.
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
     * Sets the player's status to "ready".
     * Sends readiness to the server, updates the display and buttons,
     * and marks the player as ready in the lobby.
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
     * Sets the player's status to "not ready".
     * Sends not-ready status to the server, updates the display and buttons,
     * removes the "ready" mark in the lobby, and hides the map selection
     * if the player was the map chooser.
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
     * Sends the current readiness status to the server.
     * Creates a status message and transmits it if a client is present.
     * Prints an error if no client is available.
     *
     * @param ready true if the player is ready; false otherwise.
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
     * Adds a new player to the lobby.
     * Creates a `PlayerEntry` with the given parameters and adds it to the player list,
     * if no player with this ID exists yet. The own player name is marked with "(you)".
     * Additionally, the player is added to the recipient ComboBox if it is not the own player.
     *
     * @param clientID The unique ID of the player.
     * @param name     The name of the player.
     * @param figure   The figure ID of the player.
     * @param ready    Whether the player is ready.
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
     * Processes the selection of a map by the player.
     * Sends the selected map as a message to the server,
     * if a valid selection was made.
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
     * Returns the last received list of available maps.
     * This list is updated when new maps are received from the server.
     *
     * @return List of last received maps or {@code null} if none exist.
     */
    List<String> getLastReceivedMapList() {
        return lastReceivedMapList;
    }

    /**
     * Hides the map selection.
     * Sets the visibility and management of the map selection box to invisible,
     * disables the selection button and the map ComboBox.
     * Called e.g. when the player is no longer ready.
     */
    public void hideMapSelection() {
        mapSelectionBox.setVisible(false);
        mapSelectionBox.setManaged(false);
        selectMapButton.setDisable(true);
        mapChoiceBox.setDisable(true);
    }

    /**
     * Sets the text of the map label and shows the corresponding map preview.
     * Extracts the map name from the given text and loads the corresponding preview image.
     *
     * @param text The text to display, e.g. "Selected map: MapName".
     */
    public void setMapLabel(String text) {
        mapLabel.setText(text);

        // Lade zugehöriges Bild
        String mapName = text.replace("Ausgewählte Karte: ", "").trim();
        displayMapPreview(mapName);
    }

    /**
     * Updates the readiness status of a player in the lobby.
     * Sets the player's status, adjusts the order of ready players,
     * and checks if the own player is now first.
     * If so, shows the map selection, otherwise hides it.
     * Updates the display of the player list.
     *
     * @param clientID The ID of the player whose status is changed.
     * @param isReady  Whether the player is ready.
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
     * Processes sending a chat message.
     * Reads the message from the input field, checks for empty content, and sends it to the selected recipient.
     * The message is transmitted to the server and displayed in the chat area.
     * After sending, the input field is cleared.
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
     * Updates the player list in the lobby.
     * Sets the given list as the new player list, removes all previous entries,
     * and adds the new players. The own player is not added to the recipient ComboBox.
     * Updates the display of the player list.
     *
     * @param newPlayers The new list of players to display.
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
     * Adds a chat message to the chat area.
     * The given message is appended to the text field with a line break.
     *
     * @param message The chat message to display.
     */
    public void appendChatMessage(String message) {
        chatArea.appendText(message + "\n");
    }

    /**
     * Updates the visibility and management of the ready buttons.
     * Shows the "Ready" button if the player is not ready,
     * and the "Not Ready" button if the player is ready.
     *
     * @param isReady Whether the player is ready.
     */
    private void updateReadyButtons(boolean isReady) {
        readyButton.setVisible(!isReady);
        readyButton.setManaged(!isReady);
        notReadyButton.setVisible(isReady);
        notReadyButton.setManaged(!isReady);
    }

    /**
     * Changes the name of a player in the lobby.
     * Searches for the player by the given ID and sets the new name.
     * If it is the own player, appends "(you)".
     * Updates the display of the player list after renaming.
     *
     * @param clientID The ID of the player whose name should be changed.
     * @param newName  The new name to set.
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
     * Returns a list of all players in the lobby.
     * Creates a new `PlayerEntry` object for each player, removing the "(you)" suffix from the name.
     * The returned list contains the current data of all players.
     *
     * @return List of players in the lobby without the "(you)" name suffix.
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
     * Hides a UI element with a slide-out animation.
     * Moves the given Node object down and sets its visibility and management to invisible after the animation.
     *
     * @param node The UI element to hide.
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
     * Shows a UI element with a slide-in animation.
     * Sets the given Node object to visible and managed,
     * and animates it from bottom to top to its original position.
     *
     * @param node The UI element to show.
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
     * Returns the currently set root element of the lobby UI.
     *
     * @return The stored root element.
     */
    public Parent getRoot() {
        return root;
    }

    /**
     * Sets the root element of the lobby UI.
     * Used to store the parent element for later access or adjustments.
     *
     * @param root The root element to set.
     */
    public void setRoot(Parent root) {
        this.root = root;
    }

    /**
     * Adds a hover animation to a button.
     * When hovered, the button is slightly moved up,
     * when the mouse leaves, it returns to its original position.
     *
     * @param button The button to which the animation should be added.
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
