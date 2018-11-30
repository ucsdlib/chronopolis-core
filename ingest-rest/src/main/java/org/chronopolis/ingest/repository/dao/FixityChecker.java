package org.chronopolis.ingest.repository.dao;

import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.QReplication;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.entities.storage.Fixity;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.models.enums.ReplicationStatus;
import org.chronopolis.rest.models.update.FixityUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.Optional;
import java.util.Set;

import static org.chronopolis.ingest.IngestController.hasRoleAdmin;

/**
 * Just a way to break out logic from the {@link org.chronopolis.ingest.api.ReplicationController}
 * and provide Fixity Checking for Replications. Right now both public methods are very similar in
 * their implementation, which is ok. Can clean up in the future if need be.
 *
 * @author shake
 */
public class FixityChecker {

    private final Logger log = LoggerFactory.getLogger(FixityChecker.class);

    private final StagingDao staging;
    private final ReplicationDao replications;

    public FixityChecker(StagingDao staging, ReplicationDao replicationDao) {
        this.staging = staging;
        this.replications = replicationDao;
    }

    /**
     * Validate a tagmanifest fixity value for a {@link Replication}
     *
     * @param principal the security principal of the user updating the {@link Replication}
     * @param id        the id of the {@link Replication}
     * @param update    the {@link FixityUpdate} with the computed hash
     * @return a ResponseEntity with updated Replication, if available
     */
    public ResponseEntity<Replication> checkTag(Principal principal, Long id, FixityUpdate update) {
        ResponseEntity<Replication> response;
        Replication replication = replications.findOne(QReplication.replication,
                QReplication.replication.id.eq(id));

        if (replication != null && authorized(principal, replication.getNode())) {
            replication.setReceivedTagFixity(update.getFixity());
            Bag bag = replication.getBag();

            // ok now we want to run checkFixity
            Optional<StagingStorage> bagStorageO =
                    staging.activeStorageForBag(bag, StagingDao.DISCRIMINATOR_BAG);
            Optional<StagingStorage> tokenStorageO =
                    staging.activeStorageForBag(bag, StagingDao.DISCRIMINATOR_TOKEN);

            response = bagStorageO.flatMap(bagStorage ->
                    tokenStorageO.map(tokenStorage ->
                            checkFixity(replication, bagStorage, tokenStorage)
                    )).map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(HttpStatus.CONFLICT).build());
        } else if (replication == null) {
            response = ResponseEntity.badRequest().build();
        } else {
            response = ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return response;
    }

    /**
     * Validate a tokenstore fixity value for a {@link Replication}
     *
     * @param principal the security principal of the user updating the {@link Replication}
     * @param id        the id of the {@link Replication}
     * @param update    the {@link FixityUpdate} with the computed hash
     * @return a ResponseEntity with updated Replication, if available
     */
    public ResponseEntity<Replication> checkTokenStore(Principal principal,
                                                       Long id,
                                                       FixityUpdate update) {
        ResponseEntity<Replication> response;
        Replication replication = replications.findOne(QReplication.replication,
                QReplication.replication.id.eq(id));

        if (replication != null && authorized(principal, replication.getNode())) {
            replication.setReceivedTokenFixity(update.getFixity());
            Bag bag = replication.getBag();

            // ok now we want to run checkFixity
            Optional<StagingStorage> bagStorageO =
                    staging.activeStorageForBag(bag, StagingDao.DISCRIMINATOR_BAG);
            Optional<StagingStorage> tokenStorageO =
                    staging.activeStorageForBag(bag, StagingDao.DISCRIMINATOR_TOKEN);

            response = bagStorageO.flatMap(bagStorage ->
                    tokenStorageO.map(tokenStorage ->
                            checkFixity(replication, bagStorage, tokenStorage)
                    )).map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(HttpStatus.CONFLICT).build());
        } else if (replication == null) {
            response = ResponseEntity.badRequest().build();
        } else {
            response = ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return response;
    }

    /**
     * Similar to the other authorized
     * <p>
     * Still not happy with the static method call
     *
     * @param principal
     * @param node
     * @return
     */
    private boolean authorized(Principal principal, Node node) {
        return hasRoleAdmin() || node.getUsername().equalsIgnoreCase(principal.getName());
    }

    /**
     * Check all for fixity received by a replication. If an incorrect fixity is received, update
     * the replication with the appropriate failure status. Otherwise if both fixity values have
     * been received and match, progress the replication to the next state (transferred).
     *
     * @param replication  the Replication being checked
     * @param bagStorage   the StagingStorage for the Bag data
     * @param tokenStorage the StagingStorage for the TokenStore
     * @return the Replication, possibly updated
     */
    private Replication checkFixity(Replication replication,
                                    StagingStorage bagStorage,
                                    StagingStorage tokenStorage) {
        Optional<String> tagFixity = Optional.ofNullable(replication.getReceivedTagFixity());
        Optional<String> tokenFixity = Optional.ofNullable(replication.getReceivedTokenFixity());

        // still concerned if either of these are null
        Set<Fixity> registeredTagFixity = bagStorage.getFile().getFixities();
        Set<Fixity> registeredTokenFixity = tokenStorage.getFile().getFixities();

        FixityStatus tagStatus = tagFixity.map(received -> registeredTagFixity.stream()
                .anyMatch(fixity -> fixity.getValue().equalsIgnoreCase(received)))
                .map(match -> match ? FixityStatus.MATCH : FixityStatus.MISMATCH)
                .orElse(FixityStatus.WAITING);

        FixityStatus tokenStatus = tokenFixity.map(received -> registeredTokenFixity.stream()
                .anyMatch(fixity -> fixity.getValue().equalsIgnoreCase(received)))
                .map(match -> match ? FixityStatus.MATCH : FixityStatus.MISMATCH)
                .orElse(FixityStatus.WAITING);

        if (tagStatus == FixityStatus.MATCH && tokenStatus == FixityStatus.MATCH) {
            log.info("[{} <- {}] Received matching tag and token fixity",
                    replication.getNode().getUsername(), replication.getBag().getName());
            replication.setStatus(ReplicationStatus.TRANSFERRED);
        } else if (tagStatus == FixityStatus.MISMATCH) {
            log.error("[{} <- {}] Received invalid tagmanifest fixity",
                    replication.getNode().getUsername(), replication.getBag().getName());
            replication.setStatus(ReplicationStatus.FAILURE_TAG_MANIFEST);
        } else if (tokenStatus == FixityStatus.MISMATCH) {
            log.info("[{} <- {}] Received invalid token store fixity",
                    replication.getNode().getUsername(), replication.getBag().getName());
            replication.setStatus(ReplicationStatus.FAILURE_TOKEN_STORE);
        }

        replications.save(replication);
        return replication;
    }

    private enum FixityStatus {
        MATCH, MISMATCH, WAITING
    }

}
