package org.chronopolis.rest.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Properties for connecting to an ingest api
 *
 * @author shake
 */
@ConfigurationProperties(prefix = "ingest.api")
public class IngestAPIProperties {

    /**
     * The endpoint of the Ingest Server
     */
    private String endpoint = "http://localhost:8080/ingest/";

    /**
     * The username to connect to the Ingest Server as
     */
    private String username = "ingest-user";

    /**
     * The password to use
     */
    private String password = "change-me";

    /**
     * List of nodes to replicate to
     */
    private List<String> replicateTo;

    public String getEndpoint() {
        return endpoint;
    }

    public IngestAPIProperties setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public IngestAPIProperties setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public IngestAPIProperties setPassword(String password) {
        this.password = password;
        return this;
    }

    public List<String> getReplicateTo() {
        return replicateTo;
    }

    public IngestAPIProperties setReplicateTo(List<String> replicateTo) {
        this.replicateTo = replicateTo;
        return this;
    }

}

