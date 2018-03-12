package org.chronopolis.rest.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Simple class to encapsulate a RequestBody when requesting to remove a DepositorContact
 *
 * @author shake
 */
public class DepositorContactRemove {

    private final String email;

    @JsonCreator
    public DepositorContactRemove(@JsonProperty("email") String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DepositorContactRemove that = (DepositorContactRemove) o;
        return Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }
}
