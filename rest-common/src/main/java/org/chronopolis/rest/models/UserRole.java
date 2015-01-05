package org.chronopolis.rest.models;

import com.sun.istack.internal.NotNull;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;

/**
 * Created by shake on 1/5/15.
 */
@Entity(name = "user_role")
public class UserRole {
    private Long id;

    private String username;
    private Role role;

    protected UserRole() {
    }

    public UserRole(String username, Role role) {
        this.username = username;
        this.role = role;
    }

    @Id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @NotNull
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Enumerated(EnumType.STRING)
    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }


    public enum Role {
        ADMIN, USER
    }
}
