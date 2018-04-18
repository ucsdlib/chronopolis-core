package org.chronopolis.common.storage;

/**
 * Basic class to encapsulate configuration of a Posix Staging Area in Chronopolis
 *
 * @author shake
 */
public class Posix {

    /**
     * The id held by the ingest server for this staging area
     */
    private Long id = -1L;

    /**
     * The percent at which we reject operations and issue a warning
     */
    private Double warn = 0.1;

    /**
     * The local path on disk
     */
    private String path = "/dev/null";

    /**
     * The local path for ACE
     */
    private String ace;

    public Long getId() {
        return id;
    }

    public Posix setId(Long id) {
        this.id = id;
        return this;
    }

    public Double getWarn() {
        return warn;
    }

    public Posix setWarn(Double warn) {
        this.warn = warn;
        return this;
    }

    public String getPath() {
        return path;
    }

    public Posix setPath(String path) {
        this.path = path;
        return this;
    }

    public String getAce() {
        return ace;
    }

    public Posix setAce(String ace) {
        this.ace = ace;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        // could change this to try and do posix.getPath.fileStore or smth
        Posix posix = (Posix) o;

        if (id != null ? !id.equals(posix.id) : posix.id != null) return false;
        if (path != null ? !path.equals(posix.path) : posix.path != null) return false;
        return ace != null ? ace.equals(posix.ace) : posix.ace == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (path != null ? path.hashCode() : 0);
        result = 31 * result + (ace != null ? ace.hashCode() : 0);
        return result;
    }
}
