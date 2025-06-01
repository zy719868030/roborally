package de.lmu.dbs.ifi.sep25.game;

public class Reboot extends FabricElement {
    private static Reboot instance = null;

    private Reboot() {
        super("reboot");
        // Private constructor for singleton
    }

    public static Reboot getInstance() {
        if (instance == null) {
            instance = new Reboot();
        }
        return instance;
    }

    @Override
    public void applyEffect(Robot robot) {
        // Stub: Future reboot logic (e.g., reset robot position, damage)
    }
}
