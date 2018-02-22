package org.chronopolis.rest.models;

/**
 * Input for updating a fixity
 *
 * @author shake
 */
public class FixityUpdate {

    private String fixity;

    public FixityUpdate(String fixity) {
        this.fixity = fixity;
    }

    public String getFixity() {
        return fixity;
    }

    public FixityUpdate setFixity(String fixity) {
        this.fixity = fixity;
        return this;
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
