package org.chronopolis.ingest.controller;

import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.ReplicationRepository;
import org.chronopolis.rest.models.Replication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.security.Principal;
import java.util.Collection;

/**
 * Created by shake on 4/17/15.
 */
@Controller
public class BagController {
    private final Logger log = LoggerFactory.getLogger(BagController.class);

    @Autowired
    BagRepository bagRepository;

    @Autowired
    ReplicationRepository replicationRepository;

    @RequestMapping(value= "/bags", method = RequestMethod.GET)
    public String getBags(Model model, Principal principal) {
        log.info("Getting bags for user {}", principal.getName());

        model.addAttribute("bags", bagRepository.findAll());
        // model.addAttribute("bags", new ArrayList<Bag>());
        return "bags";
    }

    @RequestMapping(value = "/replications", method = RequestMethod.GET)
    public String getReplications(Model model, Principal principal) {
        log.info("Getting replications for user {}", principal.getName());
        Collection<Replication> replications;
        if (hasRoleAdmin()) {
            replications = replicationRepository.findAll();
        } else {
            replications = replicationRepository.findByNodeUsername(principal.getName());
        }

        model.addAttribute("replications", replications);
        return "replications";
    }

    public boolean hasRoleAdmin() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        for (GrantedAuthority authority : userDetails.getAuthorities()) {
            if (authority.getAuthority().equalsIgnoreCase("ROLE_ADMIN")) {
                return true;
            }
        }

        log.debug("User does not have admin role");
        return false;
    }


}
