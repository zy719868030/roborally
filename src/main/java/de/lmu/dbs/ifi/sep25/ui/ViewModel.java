package de.lmu.dbs.ifi.sep25.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;


public class ViewModel {

    @FXML
    public VBox container;
    @FXML
    private ListView<String> list;
    @FXML
    private TextField input;
    @FXML
    private Button button;
    @FXML
    private HBox inputContainer;

    private final Model model;

    public ViewModel() {
        model = new Model();
    }

    /**
     * This method is called automatically if the FXML-Loader loads a scene (state this class as the controller
     * in the respective fxml-file) and must be called manually when using views constructed
     * with java code (e.g. {@link DemoView#DemoView()})
     */
    public void initialize() {
        // Bindings
        list.itemsProperty().set(model.getListContentProperty());
        input.textProperty().bindBidirectional(model.getTextFieldContent());
        button.disableProperty().bind(input.textProperty().isEmpty());
    }

    /**
     * Sets the node elements manually.
     * This function is only used if the view is generated in java code by {@link DemoView}.
     */
    public void setNodeElements(VBox container, ListView<String> list, TextField input, Button button) {
        this.container = container;
        this.list = list;
        this.input = input;
        this.button = button;
    }

    /**
     * This function is called if the button is pressed.
     */
    @FXML
    public void handleButtonPress(ActionEvent actionEvent) {
        model.addNewListItem();
        input.requestFocus();
    }

    @FXML
    public void handleKeyPressed(KeyEvent keyEvent){
        if (keyEvent.getCode() == KeyCode.ENTER) {
            handleButtonPress(null);
        }
    }
}


