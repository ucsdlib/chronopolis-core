package org.chronopolis.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic properties describing a StorageRegion
 *
 * @author shake
 */
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    private List<Posix> posix = new ArrayList<>();

    public List<Posix> getPosix() {
        return posix;
    }

    public StorageProperties setPosix(List<Posix> posix) {
        this.posix = posix;
        return this;
    }

    private class Posix {
        private Long id = -1L;
        private String path;

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


}
