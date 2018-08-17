package org.chronopolis.ingest.api;

import org.chronopolis.ingest.support.Loggers;
import org.chronopolis.rest.models.update.PasswordUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * REST Controller for interacting with users
 * <p>
 * <p>
 * Created by shake on 1/7/15.
 */
@RestController
@RequestMapping("/api/user")
public class UserController {
    private final Logger access = LoggerFactory.getLogger(Loggers.ACCESS_LOG);

    private final UserDetailsManager manager;

    @Autowired
    public UserController(UserDetailsManager manager) {
        this.manager = manager;
    }

    /**
     * Update a password for a user
     *
     * @param principal authentication information
     * @param update    request containing the old/new passwords
     */
    @RequestMapping(value = "password", method = RequestMethod.POST)
    public void updatePassword(Principal principal, @RequestBody PasswordUpdate update) {
        access.info("[POST /api/user/password] - {}", principal.getName());
        manager.changePassword(update.getOldPassword(), update.getNewPassword());
    }

    /**
     * Retrieve the user details for a user
     *
     * @param principal authentication information
     * @return
     */
    @RequestMapping(value = "details", method = RequestMethod.GET)
    public UserDetails getDetails(Principal principal) {
        access.info("[GET /api/user/details] - {}", principal.getName());
        return manager.loadUserByUsername(principal.getName());
    }

}
