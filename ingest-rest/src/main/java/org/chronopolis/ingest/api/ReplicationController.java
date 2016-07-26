package org.chronopolis.ingest.api;

import com.google.common.collect.ImmutableMap;
import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.exception.NotFoundException;
import org.chronopolis.ingest.repository.BagService;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.ReplicationSearchCriteria;
import org.chronopolis.ingest.repository.ReplicationService;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.models.FixityUpdate;
import org.chronopolis.rest.models.RStatusUpdate;
import org.chronopolis.rest.models.ReplicationRequest;
import org.chronopolis.rest.models.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

import static org.chronopolis.ingest.api.Params.CREATED_AFTER;
import static org.chronopolis.ingest.api.Params.CREATED_BEFORE;
import static org.chronopolis.ingest.api.Params.NODE;
import static org.chronopolis.ingest.api.Params.STATUS;
import static org.chronopolis.ingest.api.Params.UPDATED_AFTER;
import static org.chronopolis.ingest.api.Params.UPDATED_BEFORE;

/**
 * REST controller for replication methods
 * <p/>
 * TODO: We'll probably want a separate class to handle common db stuff
 * using the 3 repository classes
 * <p/>
 * Created by shake on 11/5/14.
 */
@RestController
@RequestMapping("/api/replications")
public class ReplicationController extends IngestController {
    private final Logger log = LoggerFactory.getLogger(ReplicationController.class);

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    BagService bagService;

    @Autowired
    ReplicationService replicationService;

    @Autowired
    IngestSettings settings;

    /**
     * Create a replication request for a given node and bag
     * <p/>
     * TODO: Return a 201
     *
     * @param request   - request containing the bag id to replicate
     * @return
     */
    @RequestMapping(method = RequestMethod.POST)
    public Replication createReplication(@RequestBody ReplicationRequest request) {
        log.debug("Received replication request {}", request);
        return replicationService.create(request, settings);
    }

    private ReplicationSearchCriteria createCriteria(Principal principal, Long id) {
        ReplicationSearchCriteria criteria = new ReplicationSearchCriteria()
                .withId(id);

        if (!hasRoleAdmin()) {
            criteria.withNodeUsername(principal.getName());
        }

        return criteria;
    }

    @RequestMapping(value = "/{id}/tokenstore", method = RequestMethod.PUT)
    public Replication updateTokenFixity(Principal principal,
                                         @PathVariable("id") Long replicationId,
                                         @RequestBody FixityUpdate update) {
        log.info("[{}] Updating token digest for {}", principal.getName(), replicationId);
        ReplicationSearchCriteria criteria = createCriteria(principal, replicationId);

        // Break out our objects
        Replication r = replicationService.getReplication(criteria);
        Bag bag = r.getBag();
        Node node = r.getNode();
        String fixity = update.getFixity();

        // Validate the fixity and update the replication
        checkFixity(r, bag.getId(), node.getUsername(), bag.getTokenDigest(), fixity, ReplicationStatus.FAILURE_TOKEN_STORE);
        r.setReceivedTokenFixity(fixity);
        r.checkTransferred();
        replicationService.save(r);
        return r;
    }

    @RequestMapping(value = "/{id}/tagmanifest", method = RequestMethod.PUT)
    public Replication updateTagFixity(Principal principal,
                                       @PathVariable("id") Long replicationId,
                                       @RequestBody FixityUpdate update) {
        log.info("[{}] Updating tag digest for {}", principal.getName(), replicationId);
        ReplicationSearchCriteria criteria = createCriteria(principal, replicationId);

        // Break out our objects
        Replication r = replicationService.getReplication(criteria);
        Bag bag = r.getBag();
        Node node = r.getNode();
        String fixity = update.getFixity();

        // Validate the fixity and update the replication
        checkFixity(r, bag.getId(), node.getUsername(), bag.getTagManifestDigest(), fixity, ReplicationStatus.FAILURE_TAG_MANIFEST);
        r.setReceivedTagFixity(update.getFixity());
        r.checkTransferred();
        replicationService.save(r);
        return r;
    }

    private boolean checkFixity(Replication r, Long id, String node, String stored, String received, ReplicationStatus failure) {
        if (stored == null || !stored.equals(received)) {
            log.warn("Received invalid fixity (found={},expected={}) for bag {} from {}. Setting {}", new Object[]{
                    received,
                    stored,
                    id,
                    node,
                    failure});
            r.setStatus(failure);
        } else {
            log.info("Matching fixity for {}", r.getId());
            return true;
        }

        return false;
    }


    @RequestMapping(value = "/{id}/failure", method = RequestMethod.PUT)
    public Replication failReplication(Principal principal,
                                       @PathVariable("id") Long replicationId) {
        ReplicationSearchCriteria criteria = createCriteria(principal, replicationId);
        Replication r = replicationService.getReplication(criteria);
        r.setStatus(ReplicationStatus.FAILURE);
        replicationService.save(r);
        return r;
    }

    @RequestMapping(value = "/{id}/status", method = RequestMethod.PUT)
    public Replication updateStatus(Principal principal,
                                    @PathVariable("id") Long replicationId,
                                    @RequestBody RStatusUpdate update) {
        ReplicationSearchCriteria criteria = createCriteria(principal, replicationId);
        log.info("Received update request for replication {}: {}", replicationId, update.getStatus());
        Replication r = replicationService.getReplication(criteria);
        r.setStatus(update.getStatus());
        replicationService.save(r);
        return r;
    }


    /**
     * Update a given replication based on the id of the path used
     * TODO: either create a new endpoint (../fixity) or move to the bag/repl object
     *
     * @param principal     - authentication information
     * @param replicationId - the id of the replication to update
     * @param replication   - the updated replication sent from the client
     * @return
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public Replication updateReplication(Principal principal,
                                         @PathVariable("id") Long replicationId,
                                         @RequestBody Replication replication) {
        ReplicationSearchCriteria criteria = new ReplicationSearchCriteria()
                .withId(replicationId);

        // If a user is not an admin, make sure we only search for THEIR replications
        if (!hasRoleAdmin()) {
            criteria.withNodeUsername(principal.getName());
        }
        Replication update = replicationService.getReplication(criteria);

        if (update == null) {
            throw new NotFoundException("Replication " + replicationId);
        }

        // TODO: Move logic outside of here? (yes)
        log.info("Updating replication {}", replication.getId());

        // only allow updates to nominal
        if (!update.getStatus().isFailure()) {
            update.setReceivedTokenFixity(replication.getReceivedTokenFixity());
            update.setReceivedTagFixity(replication.getReceivedTagFixity());
            if (replication.getStatus().isClientStatus()) {
                update.setStatus(replication.getStatus());
            }
        }

        replicationService.save(update);
        return update;
    }

    /**
     * Retrieve all replications associated with a particular node/user
     *
     * @param params    - query parameters used for searching
     * @return
     */
    @RequestMapping(method = RequestMethod.GET)
    public Iterable<Replication> replications(@RequestParam Map<String, String> params) {
        String name = null;

        // Workaround for giving service accounts a view into all replications
        name = params.getOrDefault(NODE, null);

        // null is handled fine so we can set that as a default
        ReplicationStatus status = params.containsKey(STATUS)
                ? ReplicationStatus.valueOf(params.get(STATUS))
                : null;

        // TODO: May want a function to build the criteria from the request params
        ReplicationSearchCriteria criteria = new ReplicationSearchCriteria()
                .createdAfter(params.getOrDefault(CREATED_AFTER, null))
                .createdBefore(params.getOrDefault(CREATED_BEFORE, null))
                .updatedAfter(params.getOrDefault(UPDATED_AFTER, null))
                .updatedBefore(params.getOrDefault(UPDATED_BEFORE, null))
                .withNodeUsername(name)
                .withStatus(status);

        PageRequest pr = createPageRequest(params, ImmutableMap.of());

        return replicationService.getReplications(criteria, pr);
    }

    /**
     * Retrieve a single replication based on its Id
     *
     * @param principal - authentication information
     * @param actionId  - the Id to search for
     * @return
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public Replication findReplication(Principal principal,
                                       @PathVariable("id") Long actionId) {
        log.info("[{}] Getting replication {}", principal.getName(), actionId);

        return replicationService.getReplication(
                new ReplicationSearchCriteria().withId(actionId)
        );
    }

    /*
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void deleteReplication(Principal principal,
                                  @PathVariable("id") Long replicationId) {
        replicationService.delete(replicationId);
    }
    */

}
