package de.lmu.dbs.ifi.sep25.card.UpgradeCard;

import de.lmu.dbs.ifi.sep25.card.Card;
import de.lmu.dbs.ifi.sep25.game.Robot;

public class Permanents extends UpgradeCard {
    private String permanentEffect;

    public Permanents(String description, String permanentEffect) {
        super(description);
        this.permanentEffect = permanentEffect;
    }

    public String getPermanentEffect() {
        return permanentEffect;
    }

    @Override
    public void execute(Robot robot) {
        //Implementation of persistent upgrade effects
        System.out.println("Robot " + robot.getId() + " activates permanent upgrade: " + permanentEffect);
    }

    @Override
    public Permanents clone() {
        return new Permanents(this.description, this.permanentEffect);
    }
}