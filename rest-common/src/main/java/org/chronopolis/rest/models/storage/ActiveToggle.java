package org.chronopolis.rest.models.storage;

public class ActiveToggle {
    private boolean active;

    public boolean isActive() {
        return active;
    }

    public ActiveToggle setActive(boolean active) {
        this.active = active;
        return this;
    }
}
