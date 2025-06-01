package de.lmu.dbs.ifi.sep25.card.DamageCard;

public class DamageCard extends Card {
    private String damageType;

    public DamageCard(String description, String damageType) {
        super(description, CardType.DAMAGE);
        this.damageType = damageType;
    }

    public String getDamageType() {
        return damageType;
    }

    @Override
    public void execute(Robot robot) {
        System.out.println("Robot " + robot.getId() + " erleidet Schadenstyp: " + damageType);
        robot.takeDamage(1);
    }

    @Override
    public DamageCard clone() {
        return new DamageCard(this.description, this.damageType);
    }
}