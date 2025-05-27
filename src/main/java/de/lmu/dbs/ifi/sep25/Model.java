package de.lmu.dbs.ifi.sep25;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Holds the data of the application and manages all the business logic
 */
public class Model {

    /**
     * Holds the list of strings we want to show on the screen.
     */
    private final ObservableList<String> listContent = FXCollections.observableArrayList();

    /**
     * This property holds the user's current input.
     */
    private final StringProperty textFieldContent = new SimpleStringProperty("");

    public Model(){
    }

    /**
     * Adds an input field's content as a new item to the list and clears the input field.
     */
    public void addNewListItem() {
        listContent.add(textFieldContent.get());
        textFieldContent.set("");
    }

    public ObservableList<String> getListContentProperty() {
        return listContent;
    }

    public StringProperty getTextFieldContent(){
        return textFieldContent;
    }
}
