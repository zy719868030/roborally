package de.lmu.dbs.ifi.sep25.game;

public abstract class FabricElement {
    private String type;

    public FabricElement(String type) {
        this.type = type;
    }

    public abstract void applyEffect(Robot robot);

    public String getType() {
        return type;
    }
}
