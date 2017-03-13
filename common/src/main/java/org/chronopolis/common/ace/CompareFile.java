package org.chronopolis.common.ace;

/**
 *
 * Created by shake on 3/13/17.
 */
public class CompareFile {

    private String path;
    private String digest;

    public String getPath() {
        return path;
    }

    public CompareFile setPath(String path) {
        this.path = path;
        return this;
    }

    public String getDigest() {
        return digest;
    }

    public CompareFile setDigest(String digest) {
        this.digest = digest;
        return this;
    }
}
