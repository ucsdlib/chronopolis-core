package org.chronopolis.rest.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by shake on 11/17/14.
 */
@Entity
public class Node {

    @Id
    @GeneratedValue
    private Long id;

    @OneToMany(mappedBy = "node")
    private Set<Replication> replications = new HashSet<>();

    @OneToMany(mappedBy = "node")
    private Set<Restoration> restorations = new HashSet<>();

    @JsonIgnore
    @Deprecated
    public String password;
    public String username;
    private boolean enabled;

    // For JPA
    protected Node() {
    }

    public Node(final String username, final String password) {
        this.password = password;
        this.username = username;
        this.enabled = true;
    }

    public Long getId() {
        return id;
    }

    public Set<Replication> getReplications() {
        return replications;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public String resourceID() {
        return "node/" + id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
