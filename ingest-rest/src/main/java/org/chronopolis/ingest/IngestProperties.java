package org.chronopolis.ingest;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author shake
 */
@ConfigurationProperties(prefix = "ingest")
public class IngestProperties {

    private Ajp ajp = new Ajp();

    private Integer fileIngestBatchSize = 1000;

    public Ajp getAjp() {
        return ajp;
    }

    public IngestProperties setAjp(Ajp ajp) {
        this.ajp = ajp;
        return this;
    }

    public Integer getFileIngestBatchSize() {
        return fileIngestBatchSize;
    }

    public IngestProperties setFileIngestBatchSize(Integer fileIngestBatchSize) {
        this.fileIngestBatchSize = fileIngestBatchSize;
        return this;
    }

    /**
     * AJP Connector configuration
     */
    public static class Ajp {
        /**
         * Flag to enable an ajp connector
         */
        private Boolean enabled = false;

        /**
         * Port to use for the connector
         */
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
