package de.lmu.dbs.ifi.sep25.card.ProgrammingCard;

import de.lmu.dbs.ifi.sep25.game.Robot;

public class RegularPro extends ProgrammingCard {
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
        robot.moveForward(distance);
    }

    @Override
    public RegularPro clone() {
        return new RegularPro(this.description, this.actionType, this.distance);
    }
}