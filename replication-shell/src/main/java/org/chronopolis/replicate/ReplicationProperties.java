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
    private Storage storage = new Storage();
    private Smtp smtp = new Smtp();

    public Storage getStorage() {
        return storage;
    }

    public ReplicationProperties setStorage(Storage storage) {
        this.storage = storage;
        return this;
    }

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

    /**
     * @deprecated This is no longer being used
     */
    public static class Storage {
        private String preservation;

        public String getPreservation() {
            return preservation;
        }

        public Storage setPreservation(String preservation) {
            this.preservation = preservation;
            return this;
        }
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
