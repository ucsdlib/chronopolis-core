package org.chronopolis.ingest.api;

import org.chronopolis.ingest.model.ReplicationAction;
import org.chronopolis.ingest.repository.ReplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Collection;

/**
 * Created by shake on 11/5/14.
 */
@RestController
@RequestMapping("/api/staging")
public class ReplicationController {

    @Autowired
    ReplicationRepository replicationRepository;

    @RequestMapping(value = "/replications", method = RequestMethod.PUT)
    public String replications(Principal principal, @RequestBody ReplicationAction action) {
        replicationRepository.save(action);
        return "ok";
    }

    @RequestMapping(value = "/replications", method = RequestMethod.GET)
    public Collection<ReplicationAction> replications(Principal principal) {
        return replicationRepository.findByNodeUsername(principal.getName());
    }

    @RequestMapping(value = "/replications/{id}")
    public ReplicationAction findReplication(@PathVariable("id") Long actionId) {
        return replicationRepository.findOne(actionId);
    }

}
