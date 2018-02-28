package org.chronopolis.rest.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

/**
 * Representation of a node in chronopolis
 *
 * TODO: Remove password/enabled
 *
 * Created by shake on 11/17/14.
 */
@Entity
public class Node extends PersistableEntity {

    @JsonIgnore
    @OneToMany(mappedBy = "node")
    private Set<Replication> replications = new HashSet<>();

    @JsonIgnore
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

    public Set<Replication> getReplications() {
        return replications;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
