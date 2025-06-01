package de.lmu.dbs.ifi.sep25.card.ProgrammingCard;

import de.lmu.dbs.ifi.sep25.game.Robot;

public class SpecialPro extends ProgrammingCard {
    private String specialEffect;

    public SpecialPro(String description, String actionType, String specialEffect) {
        super(description, actionType);
        this.specialEffect = specialEffect;
    }

    public String getSpecialEffect() {
        return specialEffect;
    }

    @Override
    public void execute(Robot robot) {
        System.out.println("Robot " + robot.getId() + " führt Spezialeffekt aus: " + specialEffect);
    }

    @Override
    public SpecialPro clone() {
        return new SpecialPro(this.description, this.actionType, this.specialEffect);
    }
}