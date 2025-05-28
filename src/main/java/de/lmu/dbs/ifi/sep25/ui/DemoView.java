package de.lmu.dbs.ifi.sep25.ui;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class DemoView extends VBox {
    public DemoView() {
        setId("container");

        // set CSS
        getStylesheets().add(getClass().getResource("/demo.css").toExternalForm());

        // creating node elements
        Label label = new Label("Created with View Class in Java");

        ListView<String> list = new ListView<>();
        list.setId("list");
        VBox.setVgrow(list, Priority.ALWAYS);

        TextField input = new TextField();
        input.setPromptText("Enter text here");
        input.setId("input");
        HBox.setHgrow(input, Priority.ALWAYS);

        Button button = new Button();
        button.setText("ADD");
        button.setId("button");

        HBox hBox = new HBox(input, button);
        hBox.setId("inputContainer");

        // adding node elements to container
        getChildren().addAll(label, list, hBox);

        // creating viewmodel
        ViewModel demoViewModel = new ViewModel();
        demoViewModel.setNodeElements(this, list, input, button);
        demoViewModel.initialize();

        // setting key pressed event to track when ENTER is pressed on the keyboard
        input.setOnKeyPressed(demoViewModel::handleKeyPressed);

        // setting button pressed event to add message to list
        button.setOnAction(demoViewModel::handleButtonPress);
    }
}

