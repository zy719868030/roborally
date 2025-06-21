package de.lmu.dbs.ifi.sep25.card.ProgrammingCard;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Player;
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
    public void execute(Robot robot, Player player) {
        if (!canExecute(robot)) return;

        Board board = robot.getBoard();
        if (board == null) return;

        switch (actionType.toLowerCase()) {
            case "move":
                robot.applyMove(board, distance);
                break;
            case "backup":
                robot.applyMove(board, -distance);
                break;
            case "turnleft":
                robot.turnLeft();
                break;
            case "turnright":
                robot.turnRight();
                break;
            case "uturn":
                robot.turnLeft();
                robot.turnLeft();
                break;
            case "powerup":
                player.addEnergy(1, "Power Up");
                break;
        }
    }

    public boolean canExecute(Robot robot) {
        return !robot.isPoweredDown();
    }

    @Override
    public RegularPro clone() {
        return new RegularPro(this.description, this.actionType, this.distance);
    }
}