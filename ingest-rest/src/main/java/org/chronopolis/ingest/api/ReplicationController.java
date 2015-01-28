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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

import static org.chronopolis.ingest.api.Params.PAGE;
import static org.chronopolis.ingest.api.Params.PAGE_SIZE;
import static org.chronopolis.ingest.api.Params.STATUS;

/**
 * TODO: We'll probably want a separate class to handle common db stuff
 * using the 3 repository classes
 * <p/>
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
            throw new NotFoundException("bag/" + request.getBagID());
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

        log.info("Updating replication {}", replication.getReplicationID());

        // check if the fixity values are non-null, and if so update them
        String receivedTokenFixity = replication.getReceivedTokenFixity();
        if (receivedTokenFixity != null) {
            update.setReceivedTokenFixity(receivedTokenFixity);
        }

        String receivedTagFixity = replication.getReceivedTagFixity();
        if (receivedTagFixity != null) {
            update.setReceivedTagFixity(receivedTagFixity);
        }

        Bag bag = update.getBag();
        String digest = bag.getTokenDigest();
        String tagDigest = bag.getTagManifestDigest();
        // these should never be null, but for the time being just check anyways
        boolean correctTokens = digest != null && digest.equals(receivedTokenFixity);
        boolean correctManifest = tagDigest != null && tagDigest.equals(receivedTagFixity);

        // if both fixities match we have a success
        // if neither fixity is null and we had at least 1 mismatch, set as failure
        if (correctTokens && correctManifest) {
            update.setStatus(ReplicationStatus.SUCCESS);
        } else if (receivedTagFixity != null && receivedTokenFixity != null) {
            update.setStatus(ReplicationStatus.FAILURE);
        } else {
            // TODO: We may just want to leave the status as STARTED
            update.setStatus(replication.getStatus());
        }

        replicationRepository.save(update);

        return update;
    }

    @RequestMapping(value = "/replications", method = RequestMethod.GET)
    public Iterable<Replication> replications(Principal principal,
                                              @RequestParam Map<String, String> params) {
        // @RequestParam(value = STATUS, required = false) ReplicationStatus status) {
        Iterable<Replication> replications;
        ReplicationStatus status = params.containsKey(STATUS) ? ReplicationStatus.valueOf(params.get(STATUS)) : null;
        Integer pageNum = params.containsKey(PAGE) ? Integer.parseInt(params.get(PAGE)) : -1;
        Integer pageSize = params.containsKey(PAGE_SIZE) ? Integer.parseInt(params.get(PAGE_SIZE)) : 20;
        String name = principal.getName();

        // TODO: maybe we can make this look a bit... cleaner.
        // if there was no page param
        if (pageNum == -1) {
            if (status == null) {
                replications = replicationRepository.findByNodeUsername(name);
            } else {
                replications = replicationRepository.findByStatusAndNodeUsername(status, name);
            }

        } else {
            Pageable pageable = new PageRequest(pageNum, pageSize);
            if (status == null) {
                replications = replicationRepository.findByNodeUsername(name, pageable);
            } else {
                replications = replicationRepository.findByStatusAndNodeUsername(status, name, pageable);
            }
        }

        return replications;
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
