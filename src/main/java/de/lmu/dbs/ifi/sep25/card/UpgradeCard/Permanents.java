package de.lmu.dbs.ifi.sep25.card.UpgradeCard;

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
        System.out.println("Robot " + robot.getId() + " aktiviert permanentes Upgrade: " + permanentEffect);
    }

    @Override
    public Permanents clone() {
        return new Permanents(this.description, this.permanentEffect);
    }
}