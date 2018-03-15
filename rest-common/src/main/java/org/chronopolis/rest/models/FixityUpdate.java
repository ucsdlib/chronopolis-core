package org.chronopolis.rest.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Input for updating a fixity
 *
 * @author shake
 */
public class FixityUpdate {

    private final String fixity;

    @JsonCreator
    public FixityUpdate(@JsonProperty("fixity") String fixity) {
        this.fixity = fixity;
    }

    public String getFixity() {
        return fixity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FixityUpdate that = (FixityUpdate) o;

        return fixity != null ? fixity.equals(that.fixity) : that.fixity == null;

    }

    @Override
    public int hashCode() {
        return fixity != null ? fixity.hashCode() : 0;
    }
}
