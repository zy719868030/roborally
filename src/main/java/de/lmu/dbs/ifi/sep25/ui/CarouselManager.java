package de.lmu.dbs.ifi.sep25.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.apache.logging.log4j.Logger;
/**
 * Manages a horizontally scrollable carousel of robot toggle buttons in a JavaFX UI.
 * <p>
 * It handles the creation of visual clones to allow seamless looping, synchronizes
 * toggle selection with scrolling, and allows navigation between available robots.
 */
public class CarouselManager {
    private static final Logger logger = org.apache.logging.log4j.LogManager.getLogger(CarouselManager.class);

    private final ScrollPane scrollPane;
    private final HBox gallery;
    private final ToggleGroup group;
    private final int robotCount;
    private static final int COPIES = 3;
    private boolean internalSelectionChange = false;

    /**
     * Constructs a new CarouselManager.
     *
     * @param scrollPane  the ScrollPane that displays the carousel content
     * @param gallery     the HBox containing the robot toggle buttons
     * @param group       the ToggleGroup managing selection of robots
     * @param robotCount  the number of unique robots
     */
    public CarouselManager(ScrollPane scrollPane, HBox gallery, ToggleGroup group, int robotCount) {
        this.scrollPane = scrollPane;
        this.gallery = gallery;
        this.group = group;
        this.robotCount = robotCount;

        initCarousel();
        initSelectionSync();
    }

    /**
     * Initializes the carousel by duplicating its content {@code COPIES} times
     * for a smooth looping visual effect.
     */
    private void initCarousel() {
        ObservableList<Node> original = FXCollections.observableArrayList(gallery.getChildren());
        gallery.getChildren().clear();

        for (int copy = 0; copy < COPIES; copy++) {
            for (Node node : original) {
                ToggleButton originalBtn = (ToggleButton) node;
                ToggleButton clone = new ToggleButton();
                clone.setUserData(originalBtn.getUserData());
                clone.setToggleGroup(group);
                clone.getStyleClass().addAll(originalBtn.getStyleClass());

                ImageView iv = new ImageView(((ImageView) originalBtn.getGraphic()).getImage());
                iv.setFitWidth(80);
                iv.setFitHeight(80);
                iv.setPreserveRatio(true);
                iv.setMouseTransparent(true);

                clone.setGraphic(iv);
                HBox.setMargin(clone, new Insets(8));

                gallery.getChildren().add(clone);
            }
        }

        Platform.runLater(() -> {
            selectRobot(0);
            scrollToRobot(0);
        });
    }

    /**
     * Initializes the synchronization between selection and scrolling.
     */
    private void initSelectionSync() {
        group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !internalSelectionChange) {
                int id = (int) newVal.getUserData();
                scrollToRobot(id);
            }
        });
    }

    /**
     * Scrolls the carousel to center the robot with the given ID.
     *
     * @param id the ID of the robot to scroll to
     */
    private void scrollToRobot(int id) {
        for (int i = robotCount; i < gallery.getChildren().size() - robotCount; i++) {
            ToggleButton btn = (ToggleButton) gallery.getChildren().get(i);
            if ((int) btn.getUserData() == id) {
                centerNode(btn);
                break;
            }
        }
        wrapToCenterCopy(id);
    }

    /**
     * Centers the given node horizontally within the scroll pane.
     *
     * @param node the node to center
     */
    private void centerNode(Node node) {
        Bounds viewport = scrollPane.getViewportBounds();
        Bounds content = gallery.getBoundsInLocal();
        Bounds nodeBounds = node.getBoundsInParent();

        double nodeCenter = nodeBounds.getMinX() + nodeBounds.getWidth() / 2;
        double target = (nodeCenter - viewport.getWidth() / 2) / (content.getWidth() - viewport.getWidth());
        scrollPane.setHvalue(clamp(target, 0, 1));
    }

    /**
     * Ensures the centered copy of the selected robot remains in view,
     * adjusting scroll position if needed.
     *
     * @param id the selected robot ID
     */
    private void wrapToCenterCopy(int id) {
        double h = scrollPane.getHvalue();
        double third = 1.0 / COPIES;

        if (h < third * 0.5) {
            scrollPane.setHvalue(h + third);
        } else if (h > 1.0 - third * 0.5) {
            scrollPane.setHvalue(h - third);
        }

        internalSelectionChange = true;
        for (int i = robotCount; i < gallery.getChildren().size() - robotCount; i++) {
            ToggleButton btn = (ToggleButton) gallery.getChildren().get(i);
            if ((int) btn.getUserData() == id) {
                btn.setSelected(true);
                break;
            }
        }
        internalSelectionChange = false;
    }

    /**
     * Clamps a double value between a minimum and maximum.
     *
     * @param val the value to clamp
     * @param min minimum allowed value
     * @param max maximum allowed value
     * @return clamped value
     */
    private double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    /**
     * Gets the currently selected robot ID.
     *
     * @return selected robot ID, or 0 if none selected
     */
    private int getSelectedId() {
        Toggle selected = group.getSelectedToggle();
        return selected != null ? (int) selected.getUserData() : 0;
    }

    /**
     * Selects the next available robot (not disabled).
     */
    public void selectNext() {
        int id = getSelectedId();
        for (int i = 1; i <= robotCount; i++) {
            int next = (id + i) % robotCount;
            if (!isRobotTaken(next)) {
                selectRobot(next);
                break;
            }
        }
    }

    /**
     * Selects the previous available robot (not disabled).
     */
    public void selectPrevious() {
        int id = getSelectedId();
        for (int i = 1; i <= robotCount; i++) {
            int prev = (id - i + robotCount) % robotCount;
            if (!isRobotTaken(prev)) {
                selectRobot(prev);
                break;
            }
        }
    }

    /**
     * Checks if the robot with the given ID is already taken (i.e., button is disabled).
     *
     * @param id robot ID
     * @return {@code true} if taken, {@code false} otherwise
     */
    private boolean isRobotTaken(int id) {
        for (Toggle toggle : group.getToggles()) {
            if ((int) toggle.getUserData() == id) {
                return toggle instanceof ToggleButton && ((ToggleButton) toggle).isDisabled();
            }
        }
        return false;
    }

    /**
     * Selects the robot with the specified ID and scrolls to it.
     *
     * @param id the ID of the robot to select
     */
    public void selectRobot(int id) {
        internalSelectionChange = true;
        try {
            for (int i = robotCount; i < gallery.getChildren().size() - robotCount; i++) {
                ToggleButton btn = (ToggleButton) gallery.getChildren().get(i);
                if ((int) btn.getUserData() == id) {
                    btn.setSelected(true);
                    scrollToRobot(id);
                    break;
                }
            }
        } finally {
            internalSelectionChange = false;
        }
    }
}
