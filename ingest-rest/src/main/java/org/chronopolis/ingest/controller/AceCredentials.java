package org.chronopolis.ingest.controller;

/**
 *
 * Created by shake on 4/19/17.
 */
public class AceCredentials {

    private String endpoint;
    private String username;
    private String password;

    public AceCredentials() {
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public AceCredentials setEndpoint(String endpoint) {
        if (endpoint != null && !endpoint.endsWith("/")) {
            endpoint += "/";
        }
        this.endpoint = endpoint;
        return this;
    }

    public AceCredentials setUsername(String username) {
        this.username = username;
        return this;
    }

    public AceCredentials setPassword(String password) {
        this.password = password;
        return this;
    }
}
