package de.lmu.dbs.ifi.sep25.card.ProgrammingCard;

import de.lmu.dbs.ifi.sep25.card.CardFactory;
import de.lmu.dbs.ifi.sep25.card.RegisterCard;
import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Game;
import de.lmu.dbs.ifi.sep25.game.Player;
import de.lmu.dbs.ifi.sep25.game.Robot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RegularPro extends ProgrammingCard {
    private final static Logger appLogger = LogManager.getLogger(RegularPro.class);

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
        if (!canExecute(robot)) {
            appLogger.error("Robot {} cannot execute regular programmed action: {}", robot.getRobotID(), actionType);
            return;
        }

        Board board = robot.getBoard();
        if (board == null) {
            appLogger.error("Robot {} cannot execute regular programmed action: {} - board is null", robot.getRobotID(), actionType);
            return;
        }

        appLogger.debug("Executing regular programmed action: {} for robot {}", actionType, robot.getRobotID());

        switch (actionType.toLowerCase()) {
            case "move" -> robot.applyMove(board, distance);
            case "backup" -> robot.applyMove(board, -distance);
            case "turnleft" -> robot.turnLeft();
            case "turnright" -> robot.turnRight();
            case "uturn" -> robot.turnAround();
            case "powerup" -> player.addEnergy(1, "Power Up");
            case "again" -> executeAgain(robot);
        }
    }

    /**
     * Repeats the action of the previous register.
     * **/
    private void executeAgain(Robot robot) {
        final Player player = findPlayerByRobot(robot);
        if (player == null) {
            appLogger.error("Cannot find player for robot {}", robot.getRobotID());
            return;
        }

        final RegisterCard prevCard = findPreviousNonAgainCard(player, Game.getInstance().getCurrentRegister());
        if (prevCard == null) {
            appLogger.error("No valid previous card to repeat for Again.");
            return;
        }

        appLogger.debug("Robot {} executing Again: Repeating {}", robot.getRobotID(), CardFactory.getCardName(prevCard));

        try {
            // Directly execute the previous card
            prevCard.execute(robot, player);
        } catch (Exception e) {
            appLogger.error("Error executing Again card: {}", e.getMessage(), e);
        }
    }

    /**
     * Recursively find the first non-Again card in previous registers
     **/
    private RegisterCard findPreviousNonAgainCard(Player player, int currentRegister) {
        // Start from the previous register
        for (int i = currentRegister - 1; i >= 0; i--) {
            RegisterCard card = player.getRegisterCard(i);
            if (card == null) {
                continue;
            }

            // Check if it's an Again card
            String cardName = CardFactory.getCardName(card);
            if (!"Again".equals(cardName) && !"RepeatRoutine".equals(cardName)) {
                // Found a non-Again card
                return card;
            }
        }

        // No valid card found in previous registers
        return null;
    }

    /**
     * Helper method to find Player by robot
     * @param robot finding the owner of this robot.
     * @return the corresponding player of the given robot.
     */
    private Player findPlayerByRobot(Robot robot) {
        Game game = Game.getInstance();
        for (Player player : game.getPlayers()) {
            if (player.getRobot() == robot) {
                return player;
            }
        }
        return null;
    }


    public boolean canExecute(Robot robot) {
        return !robot.isPoweredDown();
    }

    @Override
    public RegularPro clone() {
        return new RegularPro(this.description, this.actionType, this.distance);
    }
}