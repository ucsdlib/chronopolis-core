package org.chronopolis.rest.models.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Toggle for StagingStorage active flag of a Bag
 *
 * @author shake
 */
public class ActiveToggle {
    private final boolean active;

    @JsonCreator
    public ActiveToggle(@JsonProperty("active") boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActiveToggle toggle = (ActiveToggle) o;
        return active == toggle.active;
    }

    @Override
    public int hashCode() {

        return Objects.hash(active);
    }
}
