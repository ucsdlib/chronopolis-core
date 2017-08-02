package org.chronopolis.common.storage;

/**
 * Basic class to encapsulate configuration of a Posix Staging Area in Chronopolis
 *
 * @author shake
 */
public class Posix {

    private Long id = -1L;
    private String path = "/dev/null";

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
}
