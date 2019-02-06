package org.chronopolis.ingest;

import org.chronopolis.common.storage.Posix;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Ingest specific configuration properties
 *
 * @author shake
 */
@ConfigurationProperties(prefix = "ingest")
public class IngestProperties implements Validator {

    private Ajp ajp = new Ajp();
    private Scan scan = new Scan();
    private Tokenizer tokenizer = new Tokenizer();

    private Integer fileIngestBatchSize = 1000;

    public Scan getScan() {
        return scan;
    }

    public IngestProperties setScan(Scan scan) {
        this.scan = scan;
        return this;
    }

    public Tokenizer getTokenizer() {
        return tokenizer;
    }

    public IngestProperties setTokenizer(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        return this;
    }

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

    @Override
    public boolean supports(Class<?> clazz) {
        return IngestProperties.class == clazz;
    }

    @Override
    public void validate(Object target, Errors errors) {
        IngestProperties properties = (IngestProperties) target;

        Tokenizer tokenizerProps = properties.getTokenizer();
        if (tokenizerProps.enabled) {
            final String key = "ingest.tokenizer.staging.path";
            Path path = Paths.get(tokenizerProps.staging.getPath());
            File asFile = path.toFile();

            if (asFile == null) {
                errors.reject(key, "Path does not exist");
            } else if (!asFile.isDirectory()) {
                errors.reject(key, "Path is not a directory");
            } else if (!asFile.canRead() || !asFile.canExecute()) {
                errors.reject(key, "Cannot read/execute on given path");
            }

            if (tokenizerProps.staging.getId() <= 0) {
                errors.reject("ingest.tokenizer.staging.id", "Invalid id for StorageRegion");
            }
        }
    }

    public static class Scan {
        private Boolean enabled = false;
        private String username = "admin";
        private Posix staging = new Posix();

        public Boolean getEnabled() {
            return enabled;
        }

        public Scan setEnabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public String getUsername() {
            return username;
        }

        public Scan setUsername(String username) {
            this.username = username;
            return this;
        }

        public Posix getStaging() {
            return staging;
        }

        public Scan setStaging(Posix staging) {
            this.staging = staging;
            return this;
        }
    }

    public static class Tokenizer {

        private Boolean enabled = false;
        private String username = "admin";
        private Posix staging = new Posix();

        public String getUsername() {
            return username;
        }

        public Tokenizer setUsername(String username) {
            this.username = username;
            return this;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public Tokenizer setEnabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Posix getStaging() {
            return staging;
        }

        public Tokenizer setStaging(Posix staging) {
            this.staging = staging;
            return this;
        }
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
