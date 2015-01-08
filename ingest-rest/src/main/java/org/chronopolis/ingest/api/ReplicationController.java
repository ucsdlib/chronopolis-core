package org.chronopolis.ingest.api;

import org.chronopolis.ingest.exception.NotFoundException;
import org.chronopolis.ingest.exception.UnauthorizedException;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.ReplicationRepository;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.Node;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationRequest;
import org.chronopolis.rest.models.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Collection;

/**
 * TODO: We'll probably want a separate class to handle common db stuff
 * using the 3 repository classes
 *
 * Created by shake on 11/5/14.
 */
@RestController
@RequestMapping("/api/staging")
public class ReplicationController {
    private final Logger log = LoggerFactory.getLogger(ReplicationController.class);

    @Autowired
    ReplicationRepository replicationRepository;

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    BagRepository bagRepository;

    @RequestMapping(value = "/replications", method = RequestMethod.PUT)
    public Replication createReplication(Principal principal, @RequestBody ReplicationRequest request) {
        // Node node = nodeRepository.get()
        // Create a new replication for the Node (user) based on the Bag ID
        // Return a 404 if the bag is not found
        // If a replication already exists, return it instead of creating a new one
        Node node = nodeRepository.findByUsername(principal.getName());
        Bag bag = bagRepository.findOne(request.getBagID());

        if (bag == null) {
            throw new NotFoundException(bag.resourceID());
        }

        Replication action = replicationRepository.findByNodeUsernameAndBagID(node.getUsername(), bag.getID());

        if (action == null) {
            log.info("Creating new replication for node {} and bag {}", node.getUsername(), bag.getID());
            action = new Replication(node, bag);
            replicationRepository.save(action);
        }
        return action;
    }

    @RequestMapping(value = "/replications/{id}", method = RequestMethod.POST)
    public Replication updateReplication(Principal principal,
                                         @PathVariable("id") Long replicationID,
                                         @RequestBody Replication replication) {
        Node node = nodeRepository.findByUsername(principal.getName());
        Replication update = replicationRepository.findOne(replication.getReplicationID());

        // check for unauthorized access
        if (!update.getNode().equals(node)) {
            throw new UnauthorizedException(principal.getName());
        }

        // TODO: Ignore null values
        // TODO: check against bag fixity values
        log.info("Updating replication {}", replication.getReplicationID());
        update.setReceivedTagFixity(replication.getReceivedTagFixity());
        update.setReceivedTokenFixity(replication.getReceivedTokenFixity());
        update.setStatus(replication.getStatus());
        replicationRepository.save(update);

        return update;
    }

    @RequestMapping(value = "/replications", method = RequestMethod.GET)
    public Collection<Replication> replications(Principal principal,
                                                @RequestParam(value = "status", required = false) ReplicationStatus status) {
        if (status == null) {
            return replicationRepository.findByNodeUsername(principal.getName());
        } else {
            return replicationRepository.findByStatusAndNodeUsername(status, principal.getName());
        }
    }

    @RequestMapping(value = "/replications/{id}", method = RequestMethod.GET)
    public Replication findReplication(Principal principal, @PathVariable("id") Long actionId) {
        Replication action = replicationRepository.findOne(actionId);

        // return unauthorized
        if (!action.getNode().getUsername().equals(principal.getName())) {
            throw new UnauthorizedException(principal.getName());
        }

        return action;
    }

}
