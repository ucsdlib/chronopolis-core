package org.chronopolis.ingest.models;

/**
 * Model for creating a user
 *
 * Created by shake on 4/20/15.
 */
public class UserRequest {

    private String username;
    private String password;
    private UserRole role;
    private boolean node;

    public UserRequest(String username, String password, UserRole role, boolean node) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.node = node;
    }

    public UserRequest() {
    }

    public String getUsername() {
        return username;
    }

    public UserRequest setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public UserRequest setPassword(String password) {
        this.password = password;
        return this;
    }

    public boolean isNode() {
        return node;
    }

    public UserRequest setNode(boolean node) {
        this.node = node;
        return this;
    }

    public UserRole getRole() {
        return role;
    }

    public UserRequest setRole(UserRole role) {
        this.role = role;
        return this;
    }
}
