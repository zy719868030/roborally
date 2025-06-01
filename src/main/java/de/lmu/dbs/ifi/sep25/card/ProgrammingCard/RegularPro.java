package de.lmu.dbs.ifi.sep25.card.ProgrammingCard;

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
        robot.applyMove(distance);
    }

    @Override
    public RegularPro clone() {
        return new RegularPro(this.description, this.actionType, this.distance);
    }
}