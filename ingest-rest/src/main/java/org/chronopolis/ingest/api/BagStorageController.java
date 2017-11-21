package org.chronopolis.ingest.api;

import com.google.common.collect.ImmutableSet;
import org.chronopolis.ingest.repository.criteria.BagSearchCriteria;
import org.chronopolis.ingest.repository.dao.BagService;
import org.chronopolis.ingest.repository.dao.StagingService;
import org.chronopolis.ingest.support.Loggers;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.storage.Fixity;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.models.storage.ActiveToggle;
import org.chronopolis.rest.models.storage.FixityCreate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;

/**
 * REST controller for interacting with the Storage fields in a Bag
 * todos
 * - Constraints on PUTs (only the node/admin may alter its own resources)
 * - BadRequest on invalid type?
 */
@RestController
@RequestMapping("/api/bags/{id}")
public class BagStorageController {

    private final Logger access = LoggerFactory.getLogger(Loggers.ACCESS_LOG);

    private static final String BAG_TYPE = "bag";
    private static final String TOKEN_TYPE = "token";

    private final BagService bagService;
    private final StagingService stagingService;

    @Autowired
    public BagStorageController(BagService bagService, StagingService stagingService) {
        this.bagService = bagService;
        this.stagingService = stagingService;
    }


    /**
     * Retrieve the storage area a bag resides in
     *
     * @param id   The id of the bag
     * @param type The type of storage to retrieve
     * @return The bag's storage information
     */
    @GetMapping("/storage/{type}")
    private ResponseEntity<StagingStorage> getBagStorage(@PathVariable("id") Long id, @PathVariable("type") String type) {
        access.info("[GET /api/bags/{}/storage/{}]", id, type);
        BagSearchCriteria criteria = new BagSearchCriteria().withId(id);
        Bag bag = bagService.find(criteria);
        return storageFor(bag, type)
                .map(storage -> ResponseEntity.ok(storage))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update the activity of a bag storage area
     *
     * @param id     The id of the bag
     * @param type   The type of storage to retrieve
     * @param toggle The active flag to set
     * @return The updated Storage information
     */
    @PutMapping("/storage/{type}")
    private ResponseEntity<StagingStorage> updateStorage(@PathVariable("id") Long id, @PathVariable("type") String type, @RequestBody ActiveToggle toggle) {
        access.info("[PUT /api/bags/{}/storage/{}]", id, type);
        access.info("PUT parameters - {}", toggle.isActive());

        BagSearchCriteria criteria = new BagSearchCriteria().withId(id);
        Bag bag = bagService.find(criteria);
        Optional<StagingStorage> stagingStorage = storageFor(bag, type);

        stagingStorage.ifPresent(s -> {
            s.setActive(toggle.isActive());
            stagingService.save(s);
        });
        return stagingStorage.map(storage -> ResponseEntity.ok(storage))
                .orElse(ResponseEntity.badRequest().build());
    }

    /**
     * Retrieve all fixities associated with a Bag
     * todo: should this be a Page<Fixity>? could do storage.map(this::paged) or smth
     *
     * @param id   The id of the bag
     * @param type The type of storage to retrieve
     * @return The fixities associated with the TagManifest of the bag
     */
    @GetMapping("/storage/{type}/fixity")
    private ResponseEntity<Set<Fixity>> getFixities(@PathVariable("id") Long id, @PathVariable("type") String type) {
        access.info("[GET /api/bags/{}/storage/{}/fixity]", id, type);
        BagSearchCriteria criteria = new BagSearchCriteria().withId(id);
        Bag bag = bagService.find(criteria);
        Optional<StagingStorage> storage = storageFor(bag, type);
        return storage.map(StagingStorage::getFixities)
                .map(fixities -> ResponseEntity.ok(fixities))
                .orElse(ResponseEntity.ok(ImmutableSet.of())); // no 404, just an empty set
    }

    /**
     * Add a fixity to a Bag
     *
     * @param id     the id of the bag
     * @param type   The type of storage to retrieve
     * @param create the fixity to create
     * @return the newly created fixity
     */
    @PutMapping("/storage/{type}/fixity")
    private ResponseEntity<Fixity> addFixity(@PathVariable("id") Long id, @PathVariable("type") String type, @RequestBody FixityCreate create) {
        access.info("[PUT /api/bags/{}/storage/{}/fixity]", id, type);
        access.info("Put parameters - {};{}", create.getAlgorithm(), create.getValue());

        Fixity fixity = new Fixity()
                .setValue(create.getValue())
                .setAlgorithm(create.getAlgorithm())
                .setCreatedAt(ZonedDateTime.now());

        BagSearchCriteria criteria = new BagSearchCriteria().withId(id);
        Bag bag = bagService.find(criteria);
        Optional<StagingStorage> stagingStorage = storageFor(bag, type);

        // todo: might need 409/401 responses too
        return stagingStorage.map(s -> saveFixity(s, fixity))
                .map(f -> ResponseEntity.status(HttpStatus.CREATED).body(fixity))
                .orElse(ResponseEntity.badRequest().build());
    }

    private Fixity saveFixity(StagingStorage storage, Fixity fixity) {
        storage.addFixity(fixity);
        fixity.setStorage(storage);
        stagingService.save(storage);
        return fixity;
    }

    /**
     * Retrieve a fixity for a bag identified by its algorithm
     * <p>
     * todo: the db should really be pulling the fixity object out; when we look into
     * using more of the querydsl functionality we should be able to do that
     *
     * @param id        The id of the bag
     * @param type      The type of storage to retrieve
     * @param algorithm The algorithm used to compute the fixity
     * @return The fixity value for the algorithm, if it exists
     */
    @GetMapping("/storage/{type}/fixity/{alg}")
    private ResponseEntity<Fixity> getFixity(@PathVariable("id") Long id, @PathVariable("type") String type, @PathVariable("alg") String algorithm) {
        access.info("[GET /api/bags/{}/storage/{}/fixity/{alg}]", id, type, algorithm);

        BagSearchCriteria criteria = new BagSearchCriteria().withId(id);
        Bag bag = bagService.find(criteria);
        Optional<StagingStorage> storage = storageFor(bag, type);
        return storage.map(StagingStorage::getFixities)
                // this.... sucks
                .flatMap(fixities -> fixities.stream()
                        .filter(f -> f.getAlgorithm().equalsIgnoreCase(algorithm))
                        .findFirst())
                .map(fixity -> ResponseEntity.ok(fixity))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieve the StagingStorage for a given Bag and type, or Optional.empty if it does not exist
     *
     * @param bag  the bag to retrieve the StagingStorage object from
     * @param type the type of StagingStorage to retrieve
     * @return the StagingStorage
     */
    private Optional<StagingStorage> storageFor(Bag bag, String type) {
        Optional<StagingStorage> storage = Optional.empty();

        if (TOKEN_TYPE.equalsIgnoreCase(type)) {
            storage = stagingService.activeStorageForBag(bag, QBag.bag.tokenStorage);
        } else if (BAG_TYPE.equalsIgnoreCase(type)) {
            storage = stagingService.activeStorageForBag(bag, QBag.bag.bagStorage);
        }

        return storage;
    }

}
