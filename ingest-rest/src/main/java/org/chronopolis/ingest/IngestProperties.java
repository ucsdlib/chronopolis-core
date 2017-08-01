package org.chronopolis.ingest;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author shake
 */
@ConfigurationProperties(prefix = "ingest")
public class IngestProperties {

    private Ajp ajp = new Ajp();

    public Ajp getAjp() {
        return ajp;
    }

    public IngestProperties setAjp(Ajp ajp) {
        this.ajp = ajp;
        return this;
    }

    public static class Ajp {
        private Boolean enabled = false;
        private Integer port = 8009;

        public Boolean isEnabled() {
            return enabled;
        }

        public Ajp setEnabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Integer getPort() {
            return port;
        }

        public Ajp setPort(Integer port) {
            this.port = port;
            return this;
        }
    }
}
