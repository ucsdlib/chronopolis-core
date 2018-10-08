package org.chronopolis.replicate;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ConfigurationProperties for replication
 *
 * At the moment we'll keep this close to a mapping from what it previously was,
 * then we'll make enhancements.
 *
 * @author shake
 */
@ConfigurationProperties(prefix = "chron")
public class ReplicationProperties {

    /**
     * Node name to use for sending mail
     */
    private String node = "chron";

    /**
     * Work directory for temporary files
     */
    private String workDirectory = "/tmp/chronopolis";

    /**
     * The maximum number of file transfers to execute at a single time
     */
    private Integer maxFileTransfers = 2;

    /**
     * Smtp configuration
     */
    private Smtp smtp = new Smtp();

    public Smtp getSmtp() {
        return smtp;
    }

    public ReplicationProperties setSmtp(Smtp smtp) {
        this.smtp = smtp;
        return this;
    }

    public String getNode() {
        return node;
    }

    public ReplicationProperties setNode(String node) {
        this.node = node;
        return this;
    }

    public String getWorkDirectory() {
        return workDirectory;
    }

    public ReplicationProperties setWorkDirectory(String workDirectory) {
        this.workDirectory = workDirectory;
        return this;
    }

    public Integer getMaxFileTransfers() {
        return maxFileTransfers;
    }

    public ReplicationProperties setMaxFileTransfers(Integer maxFileTransfers) {
        this.maxFileTransfers = maxFileTransfers;
        return this;
    }

    /**
     * Additional smtp configuration unique to the replication shell
     */
    public static class Smtp {
        /**
         * Flag to enable sending notification on successful replications
         */
        private Boolean sendOnSuccess = false;

        public Boolean getSendOnSuccess() {
            return sendOnSuccess;
        }

        public Smtp setSendOnSuccess(Boolean sendOnSuccess) {
            this.sendOnSuccess = sendOnSuccess;
            return this;
        }
    }


}
