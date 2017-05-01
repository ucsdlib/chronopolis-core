package org.chronopolis.ingest.repository.dao;

import org.chronopolis.ingest.models.UserRequest;
import org.chronopolis.ingest.repository.AuthoritiesRepository;
import org.chronopolis.ingest.repository.Authority;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.models.PasswordUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.security.Principal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Service to handle things related to user accounts
 *
 * Created by shake on 6/10/15.
 */
@Component
@Transactional
public class UserService {

    private final Logger log = LoggerFactory.getLogger(UserService.class);

    private final AuthoritiesRepository authorities;
    private final UserDetailsManager manager;
    private final NodeRepository repository;
    private final PasswordEncoder encoder;

    @Autowired
    public UserService(AuthoritiesRepository authorities,
                       UserDetailsManager manager,
                       NodeRepository repository,
                       PasswordEncoder encoder) {
        this.authorities = authorities;
        this.manager = manager;
        this.repository = repository;
        this.encoder = encoder;
    }

    public void createUser(UserRequest request) {
        if (manager.userExists(request.getUsername())) {
            log.debug("User already exists, avoiding creation");
            return;
        }

        log.info("Creating new user {}", request.getUsername());
        String username = request.getUsername();
        String password = encoder.encode(request.getPassword());

        Collection<SimpleGrantedAuthority> authorities = new HashSet<>();

        authorities.add(new SimpleGrantedAuthority(request.getRole().name()));
        UserDetails userDetails = new User(username, password, authorities);
        manager.createUser(userDetails);

        // Add node if requested
        if (request.isNode()) {
            log.debug("Creating node for {}", username);
            if (repository.findByUsername(username) == null) {
                repository.save(new Node(username, password));
            }
        }
    }

    public Authority getUserAuthority(String user) {
        return authorities.findOne(user);
    }

    public List<Authority> listUserAuthorities() {
        return authorities.findAll();
    }

    UserDetails getUser(String username) {
        return manager.loadUserByUsername(username);
    }

    /**
     * Update the password for a user (who is already authenticated)
     *
     * TODO: I don't really like using the UserDetailsManager for this, it redirects us
     *       back to the login page which can cause all sorts of chaos. We should look
     *       for an alternative so that if a pw update fails we don't enter some weird
     *       redirect/bad credentials loop.
     *
     * @param update the password update
     * @param principal the security principal of the user
     */
    public void updatePassword(PasswordUpdate update, Principal principal) {
        log.info("Updating password for user {}", principal.getName());
        manager.changePassword(update.getOldPassword(), encoder.encode(update.getNewPassword()));
    }
}
