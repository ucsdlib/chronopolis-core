package org.chronopolis.ingest.repository;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Class representing authorities from spring security
 * Pretty much as bare as it can be
 *
 * Might want to add in a User class as well so we can query that table too
 *
 * Created by shake on 3/24/16.
 */
@Entity
@Table(name = "authorities")
public class Authority {

    @Id
    private String username;
    private String authority;

    public Authority() {
    }

    public String getUsername() {
        return username;
    }

    public Authority setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getAuthority() {
        return authority;
    }

    public Authority setAuthority(String authority) {
        this.authority = authority;
        return this;
    }
}
