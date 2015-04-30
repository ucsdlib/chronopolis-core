package org.chronopolis.ingest.api;

import org.chronopolis.ingest.exception.NotFoundException;
import org.chronopolis.ingest.exception.UnauthorizedException;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.ReplicationRepository;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.BagStatus;
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
import java.util.Set;

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
@RequestMapping("/api/replications")
public class ReplicationController {
    private final Logger log = LoggerFactory.getLogger(ReplicationController.class);

    @Autowired
    ReplicationRepository replicationRepository;

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    BagRepository bagRepository;

    @RequestMapping(method = RequestMethod.PUT)
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

    @RequestMapping(value = "/{id}", method = RequestMethod.POST)
    public Replication updateReplication(Principal principal,
                                         @PathVariable("id") Long replicationID,
                                         @RequestBody Replication replication) {
        Node node = nodeRepository.findByUsername(principal.getName());
        Replication update = replicationRepository.findOne(replication.getID());
        Bag bag = update.getBag();

        // check for unauthorized access
        if (!update.getNode().equals(node)) {
            throw new UnauthorizedException(principal.getName());
        }

        log.info("Updating replication {}", replication.getID());

        String receivedTokenFixity = replication.getReceivedTokenFixity();
        String receivedTagFixity = replication.getReceivedTagFixity();
        boolean success = true;

        // Only update status if we were given a fixity value
        if (receivedTokenFixity != null) {
            log.debug("Received token fixity of {}", receivedTokenFixity);
            update.setReceivedTokenFixity(receivedTokenFixity);
            // Check against the stored digest
            if (!bag.getTokenDigest().equals(receivedTokenFixity)) {
                // log.info("Received invalid token store fixity for bag {} from {}");
                update.setStatus(ReplicationStatus.FAILURE_TOKEN_STORE);
                success = false;
            }
        } else {
            success = false;
        }

        if (receivedTagFixity != null) {
            log.debug("Received tag fixity of {}", receivedTagFixity);
            update.setReceivedTagFixity(receivedTagFixity);
            // Check against the stored digest
            if (!bag.getTagManifestDigest().equals(receivedTagFixity)) {
                // log.info("Received invalid tagmanifest fixity for bag {} from {}");
                update.setStatus(ReplicationStatus.FAILURE_TAG_MANIFEST);
                success = false;
            }
        } else {
            success = false;
        }

        // If we were able to validate all the manifests: yay
        // else check if the replicating node reported any problems
        if (success) {
            update.setStatus(ReplicationStatus.SUCCESS);
            Set<Node> nodes = bag.getReplicatingNodes();
            nodes.add(node);

            // And last check to see if the bag has been replicated
            if (nodes.size() >= bag.getRequiredReplications()) {
                bag.setStatus(BagStatus.REPLICATED);
            }
        } else if (replication.getStatus() == ReplicationStatus.FAILURE){
            // log.info("Received error for replication {} from {}");
            update.setStatus(ReplicationStatus.FAILURE);
        }

        replicationRepository.save(update);
        bagRepository.save(bag);

        return update;
    }

    @RequestMapping(method = RequestMethod.GET)
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

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public Replication findReplication(Principal principal, @PathVariable("id") Long actionId) {
        Replication action = replicationRepository.findOne(actionId);

        // return unauthorized
        if (!action.getNode().getUsername().equals(principal.getName())) {
            throw new UnauthorizedException(principal.getName());
        }

        return action;
    }

}
