package org.chronopolis.ingest.api;

import com.google.common.collect.ImmutableMap;
import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.exception.NotFoundException;
import org.chronopolis.ingest.repository.BagService;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.ReplicationSearchCriteria;
import org.chronopolis.ingest.repository.ReplicationService;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.FixityUpdate;
import org.chronopolis.rest.models.Node;
import org.chronopolis.rest.models.RStatusUpdate;
import org.chronopolis.rest.models.Replication;
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

import static org.chronopolis.ingest.api.Params.STATUS;

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
        log.info("[{}] Updating token store for {}", principal.getName(), replicationId);
        ReplicationSearchCriteria criteria = createCriteria(principal, replicationId);
        Replication r = replicationService.getReplication(criteria);
        r.setReceivedTokenFixity(update.getFixity());
        replicationService.save(r);
        return r;
    }

    @RequestMapping(value = "/{id}/tagmanifest", method = RequestMethod.PUT)
    public Replication updateTagFixity(Principal principal,
                                       @PathVariable("id") Long replicationId,
                                       @RequestBody FixityUpdate update) {
        ReplicationSearchCriteria criteria = createCriteria(principal, replicationId);
        Replication r = replicationService.getReplication(criteria);
        r.setReceivedTagFixity(update.getFixity());
        replicationService.save(r);
        return r;
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

        Node node = update.getNode();
        Bag bag = update.getBag();

        // TODO: Move logic outside of here? (yes)
        log.info("Updating replication {}", replication.getId());

        // only allow updates to nominal
        if (!isFailureStatus(update.getStatus())) {
            update.setReceivedTokenFixity(replication.getReceivedTokenFixity());
            update.setReceivedTagFixity(replication.getReceivedTagFixity());
            if (isClientStatus(replication.getStatus())) {
                update.setStatus(replication.getStatus());
            }
        }

        replicationService.save(update);
        return update;
    }

    /**
     * Return true if the status = success
     *
     * @param status
     * @return
     */
    private boolean isClientSuccess(ReplicationStatus status) {
        return status == ReplicationStatus.SUCCESS;
    }

    /**
     * Return true if this is a status set by the client
     *
     * TODO: This can be done in the enumerated type.
     *
     * @param status
     * @return
     */
    private boolean isClientStatus(ReplicationStatus status) {
        return status == ReplicationStatus.STARTED
            || status == ReplicationStatus.TRANSFERRED
            || status == ReplicationStatus.FAILURE;
    }

    /**
     * Return true if the status is a failure mode set by the server
     *
     * @param status
     * @return
     */
    private boolean isFailureStatus(ReplicationStatus status) {
        return status == ReplicationStatus.FAILURE_TOKEN_STORE
            || status == ReplicationStatus.FAILURE_TAG_MANIFEST
            || status == ReplicationStatus.FAILURE;
    }

    /**
     * Retrieve all replications associated with a particular node/user
     *
     * @param principal - authentication information
     * @param params    - query parameters used for searching
     * @return
     */
    @RequestMapping(method = RequestMethod.GET)
    public Iterable<Replication> replications(Principal principal,
                                              @RequestParam Map<String, String> params) {
        String name = null;

        // Workaround for giving service accounts a view into all replications
        // TODO: Add request param for name
        Node node = nodeRepository.findByUsername(principal.getName());
        if (node != null) {
            name = principal.getName();
        }

        // null is handled fine so we can set that as a default
        ReplicationStatus status = params.containsKey(STATUS)
                ? ReplicationStatus.valueOf(params.get(STATUS))
                : null;

        // TODO: May want a function to build the criteria from the request params
        ReplicationSearchCriteria criteria = new ReplicationSearchCriteria()
                .withNodeUsername(name)
                .withStatus(status);

        PageRequest pr = createPageRequest(params, ImmutableMap.<String, String>of());

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
        Replication action = replicationService.getReplication(
                new ReplicationSearchCriteria().withId(actionId)
        );

        return action;
    }

    /*
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void deleteReplication(Principal principal,
                                  @PathVariable("id") Long replicationId) {
        replicationService.delete(replicationId);
    }
    */

}
