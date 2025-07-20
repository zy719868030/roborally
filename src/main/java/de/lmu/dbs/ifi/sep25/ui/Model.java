package de.lmu.dbs.ifi.sep25.ui;

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

    /**
     * Returns the observable list containing the list content.
     * <p>
     * This list can be bound to UI elements like ListView to reflect changes automatically.
     *
     * @return An {@code ObservableList<String>} representing the list content.
     */
    public ObservableList<String> getListContentProperty() {
        return listContent;
    }

    /**
     * Returns the {@code StringProperty} bound to the text field content.
     * <p>
     * Useful for data binding in JavaFX to keep UI and data in sync.
     *
     * @return The {@code StringProperty} representing the text field content.
     */
    public StringProperty getTextFieldContent(){
        return textFieldContent;
    }
}
