package de.lmu.dbs.ifi.sep25.card.DamageCard;

import de.lmu.dbs.ifi.sep25.card.Card;
import de.lmu.dbs.ifi.sep25.game.Robot;

public class DamageCard extends Card {
    private String damageType;

    public DamageCard(String description, String damageType) {
        super(description, CardType.DAMAGE);
        this.damageType = damageType;
    }

    // Get the type of this damage card
    public String getDamageType() {
        return damageType;
    }

    @Override
    public void execute(Robot robot) {
        System.out.println("Robot " + robot.getId() + " suffers damage type: " + damageType);
        //Make the robot lose 1 life or trigger the injury logic.
        robot.takeDamage(1);
    }

    @Override
    public DamageCard clone() {
        return new DamageCard(this.description, this.damageType);
    }
}