package org.chronopolis.rest.models;

/**
 * Request for updating a password
 *
 * Need old/new because that's what the UserDetailsManager expects
 *
 * Created by shake on 1/7/15.
 */
public class PasswordUpdate {

    String oldPassword;
    String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
