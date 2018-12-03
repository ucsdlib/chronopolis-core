package org.chronopolis.ingest.api;

import com.querydsl.core.types.dsl.BooleanExpression;
import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.exception.NotFoundException;
import org.chronopolis.ingest.models.filter.ReplicationFilter;
import org.chronopolis.ingest.repository.dao.FixityChecker;
import org.chronopolis.ingest.repository.dao.ReplicationDao;
import org.chronopolis.ingest.repository.dao.StagingDao;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.QReplication;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.entities.projections.ReplicationView;
import org.chronopolis.rest.models.create.ReplicationCreate;
import org.chronopolis.rest.models.enums.ReplicationStatus;
import org.chronopolis.rest.models.update.FixityUpdate;
import org.chronopolis.rest.models.update.ReplicationStatusUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * REST controller for replication methods
 * <p>
 * Created by shake on 11/5/14.
 */
@RestController
@RequestMapping("/api/replications")
public class ReplicationController extends IngestController {
    private final Logger log = LoggerFactory.getLogger(ReplicationController.class);

    private final StagingDao stagingDao;
    private final ReplicationDao replicationDao;

    @Autowired
    public ReplicationController(StagingDao stagingDao,
                                 ReplicationDao replicationDao) {
        this.stagingDao = stagingDao;
        this.replicationDao = replicationDao;
    }

    /**
     * Create a replication request for a given node and bag
     * <p/>
     *
     * @param request request containing the bag id to replicate
     * @return 201 with the newly created Replication
     *         400 if the request is not valid
     */
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Replication> createReplication(@RequestBody ReplicationCreate request) {
        log.debug("Received replication request {}", request);
        ResponseEntity<Replication> response;
        response = replicationDao.create(request)
                .getResult()
                .map(replication -> ResponseEntity.status(HttpStatus.CREATED).body(replication))
                .orElse(ResponseEntity.badRequest().build());
        return response;
    }

    /**
     * Update the received fixity for a token store
     * <p>
     * todo: could do /{id}/fixity/token
     *
     * @param principal     the principal of the user
     * @param replicationId the id of the replication
     * @param update        the update to apply
     * @return the updated replication
     */
    @RequestMapping(value = "/{id}/tokenstore", method = RequestMethod.PUT)
    public ResponseEntity<Replication> updateTokenFixity(Principal principal,
                                                         @PathVariable("id") Long replicationId,
                                                         @RequestBody FixityUpdate update) {
        FixityChecker checker = new FixityChecker(stagingDao, replicationDao);
        return checker.checkTokenStore(principal, replicationId, update);
    }

    /**
     * Update the received fixity for a tag manifest
     *
     * @param principal     the principal of the user
     * @param replicationId the id of the replication
     * @param update        the update to apply
     * @return the updated replication
     */
    @RequestMapping(value = "/{id}/tagmanifest", method = RequestMethod.PUT)
    public ResponseEntity<Replication> updateTagFixity(Principal principal,
                                                       @PathVariable("id") Long replicationId,
                                                       @RequestBody FixityUpdate update) {
        FixityChecker checker = new FixityChecker(stagingDao, replicationDao);
        return checker.checkTag(principal, replicationId, update);
    }

    @RequestMapping(value = "/{id}/failure", method = RequestMethod.PUT)
    public Replication failReplication(Principal principal,
                                       @PathVariable("id") Long replicationId) {
        Replication replication = replicationDao.findOne(QReplication.replication,
                QReplication.replication.id.eq(replicationId));
        replication.setStatus(ReplicationStatus.FAILURE);
        replicationDao.save(replication);
        return replication;
    }

    @RequestMapping(value = "/{id}/status", method = RequestMethod.PUT)
    public Replication updateStatus(Principal principal,
                                    @PathVariable("id") Long replicationId,
                                    @RequestBody ReplicationStatusUpdate update) {
        log.info("Received update request for replication {}: {}",
                replicationId, update.getStatus());
        Replication replication = replicationDao.findOne(QReplication.replication,
                QReplication.replication.id.eq(replicationId));
        replication.setStatus(update.getStatus());
        replicationDao.save(replication);
        return replication;
    }


    /**
     * Update a given replication based on the id of the path used
     * TODO: either create a new endpoint (../fixity) or move to the bag/repl object
     *
     * @param principal     authentication information
     * @param replicationId the id of the replication to update
     * @param replication   the updated replication sent from the client
     * @return the updated replication
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public Replication updateReplication(Principal principal,
                                         @PathVariable("id") Long replicationId,
                                         @RequestBody Replication replication) {
        BooleanExpression query = QReplication.replication.id.eq(replicationId);

        // If a user is not an admin, make sure we only search for THEIR replications
        if (!hasRoleAdmin()) {
            query = query.and(QReplication.replication.node.username.eq(principal.getName()));
        }

        Replication update = replicationDao.findOne(QReplication.replication, query);

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

        replicationDao.save(update);
        return update;
    }

    /**
     * Retrieve all replications associated with a particular node/user
     *
     * @param filter query parameters used for searching
     * @return all replication matching the request parameters
     */
    @RequestMapping(method = RequestMethod.GET)
    public Iterable<ReplicationView> replications(@ModelAttribute ReplicationFilter filter) {
        return replicationDao.findViewsAsPage(filter);
    }

    /**
     * Retrieve a single replication based on its Id
     * <p>
     *
     * @param principal authentication information
     * @param id        the id to search for
     * @return the replication specified by the id
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<ReplicationView> findReplication(Principal principal,
                                                           @PathVariable("id") Long id) {
        ResponseEntity<ReplicationView> entity = ResponseEntity.notFound().build();
        ReplicationView view = replicationDao.findReplicationAsView(id);
        if (view != null) {
            entity = ResponseEntity.ok(view);
        }
        return entity;
    }

}
