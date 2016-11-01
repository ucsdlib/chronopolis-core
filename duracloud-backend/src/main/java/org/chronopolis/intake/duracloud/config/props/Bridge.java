package org.chronopolis.intake.duracloud.config.props;

/**
 *
 * Created by shake on 10/31/16.
 */
public class Bridge {

    private String username = "bridge";
    private String password;
    private String endpoint = "localhost:8000";

    public String getUsername() {
        return username;
    }

    public Bridge setUsername(String user) {
        this.username = user;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public Bridge setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public Bridge setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }
}
