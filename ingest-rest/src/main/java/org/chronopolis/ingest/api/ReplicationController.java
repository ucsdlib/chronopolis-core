package org.chronopolis.ingest.api;

import org.chronopolis.ingest.exception.BagNotFoundException;
import org.chronopolis.ingest.model.Bag;
import org.chronopolis.ingest.model.Node;
import org.chronopolis.ingest.model.ReplicationAction;
import org.chronopolis.ingest.model.ReplicationRequest;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.NodeRepository;
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
 * TODO: We'll probably want a separate class to handle common db stuff
 * using the 3 repository classes
 *
 * Created by shake on 11/5/14.
 */
@RestController
@RequestMapping("/api/staging")
public class ReplicationController {

    @Autowired
    ReplicationRepository replicationRepository;

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    BagRepository bagRepository;

    @RequestMapping(value = "/replications", method = RequestMethod.PUT)
    public String createReplication(Principal principal, @RequestBody ReplicationRequest request) {
        // Node node = nodeRepository.get()
        // Create a new replication for the Node (user) based on the Bag ID
        // Return a 404 if the bag is not found
        Node node = nodeRepository.findByUsername(principal.getName());
        Bag bag = bagRepository.findById(request.getBagID());

        if (bag == null) {
            throw new BagNotFoundException(request.getBagID());
        }

        ReplicationAction action = new ReplicationAction(node,
                request.getBagID(),
                bag.getTagManifestDigest(),
                bag.getTokenDigest());
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
