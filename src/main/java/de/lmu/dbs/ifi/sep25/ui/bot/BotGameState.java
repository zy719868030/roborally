package de.lmu.dbs.ifi.sep25.ui.bot;

import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

import java.util.List;

public record BotGameState(
        RobotInfo me,
        List<Position> checkpoints,
        List<List<List<MessageDefinitions.Field>>> boardMap
) {

}
