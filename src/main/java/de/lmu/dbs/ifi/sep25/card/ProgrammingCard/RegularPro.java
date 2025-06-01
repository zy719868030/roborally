package de.lmu.dbs.ifi.sep25.card.ProgrammingCard;

import de.lmu.dbs.ifi.sep25.card.Card;
import de.lmu.dbs.ifi.sep25.game.Robot;

public class RegularPro extends ProgrammingCard {
    // Indicates the distance traveled, e.g., 1, 2, or 3 frames forward.
    private int distance;

    public RegularPro(String description, String actionType, int distance) {
        super(description, actionType);
        this.distance = distance;
    }

    public int getDistance() {
        return distance;
    }

    @Override
    public void execute(Robot robot) {
        robot.applyMove(distance);
    }

    @Override
    public RegularPro clone() {
        return new RegularPro(this.description, this.actionType, this.distance);
    }
}