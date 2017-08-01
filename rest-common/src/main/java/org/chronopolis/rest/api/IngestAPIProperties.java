package org.chronopolis.rest.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for connecting to an ingest api
 *
 * @author shake
 */
@ConfigurationProperties(prefix = "ingest.api")
public class IngestAPIProperties {

    private String endpoint = "http://localhost:8080/ingest/";
    private String username = "ingest-user";
    private String password = "change-me";

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
}
