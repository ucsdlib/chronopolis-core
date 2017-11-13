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
}
