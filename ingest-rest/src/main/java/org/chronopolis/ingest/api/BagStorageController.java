package org.chronopolis.ingest.api;

import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.criteria.BagSearchCriteria;
import org.chronopolis.ingest.repository.dao.SearchService;
import org.chronopolis.ingest.support.Loggers;
import org.chronopolis.rest.entities.Bag;
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
 *  - Constraints on PUTs (only the node/admin may alter its own resources)
 *
 */
@RestController
@RequestMapping("/api/bags/{id}")
public class BagStorageController {

    private final Logger access = LoggerFactory.getLogger(Loggers.ACCESS_LOG);

    private static final String BAG_TYPE = "bag";
    private static final String TOKEN_TYPE = "token";

    private final NodeRepository nodeRepository;
    private final SearchService<Bag, Long, BagRepository> bagService;

    @Autowired
    public BagStorageController(NodeRepository nodeRepository, SearchService<Bag, Long, BagRepository> bagService) {
        this.nodeRepository = nodeRepository;
        this.bagService = bagService;
    }


    /**
     * Retrieve the storage area a bag resides in
     *
     * @param id The id of the bag
     * @param type The type of storage to retrieve
     * @return The bag's storage information
     */
    @GetMapping("/storage/{type}")
    private StagingStorage getBagStorage(@PathVariable("id") Long id, @PathVariable("type") String type) {
        access.info("[GET /api/bags/{}/storage/{}]", id, type);

        StagingStorage storage = null;
        BagSearchCriteria criteria = new BagSearchCriteria().withId(id);
        Bag bag = bagService.find(criteria);
        if (TOKEN_TYPE.equalsIgnoreCase(type)) {
            storage = bag.getTokenStorage();
        } else if (BAG_TYPE.equalsIgnoreCase(type)) {
            storage = bag.getBagStorage();
        }
        return storage;
    }

    /**
     * Update the activity of a bag storage area
     *
     * @param id The id of the bag
     * @param type The type of storage to retrieve
     * @param toggle The active flag to set
     * @return The updated Storage information
     */
    @PutMapping("/storage/{type}")
    private StagingStorage updateStorage(@PathVariable("id") Long id, @PathVariable("type") String type, @RequestBody ActiveToggle toggle) {
        access.info("[PUT /api/bags/{}/storage/{}]", id, type);
        access.info("PUT parameters - {}", toggle.isActive());
        BagSearchCriteria criteria = new BagSearchCriteria().withId(id);
        Bag bag = bagService.find(criteria);
        if (TOKEN_TYPE.equalsIgnoreCase(type)) {
            bag.getTokenStorage().setActive(toggle.isActive());
        } else if (BAG_TYPE.equalsIgnoreCase(type)) {
            bag.getBagStorage().setActive(toggle.isActive());
        }
        bagService.save(bag);
        return bag.getBagStorage();
    }

    /**
     * Retrieve all fixities associated with a Bag
     * todo: should this be a Page<Fixity>?
     *
     * @param id The id of the bag
     * @param type The type of storage to retrieve
     * @return The fixities associated with the TagManifest of the bag
     */
    @GetMapping("/storage/{type}/fixity")
    private Set<Fixity> getFixities(@PathVariable("id") Long id, @PathVariable("type") String type) {
        access.info("[GET /api/bags/{}/storage/{}/fixity]", id, type);
        Set<Fixity> fixities = null;
        BagSearchCriteria criteria = new BagSearchCriteria().withId(id);
        Bag bag = bagService.find(criteria);

        if (BAG_TYPE.equalsIgnoreCase(type)) {
            fixities = bag.getBagStorage().getFixities();
        } else if (TOKEN_TYPE.equalsIgnoreCase(type)) {
            fixities = bag.getTokenStorage().getFixities();
        }

        return fixities;
    }

    /**
     * Add a fixity to a Bag
     *
     * @param id the id of the bag
     * @param type The type of storage to retrieve
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
        ResponseEntity<Fixity> response = ResponseEntity.status(HttpStatus.CREATED)
                .body(fixity);

        BagSearchCriteria criteria = new BagSearchCriteria().withId(id);
        Bag bag = bagService.find(criteria);

        if (TOKEN_TYPE.equalsIgnoreCase(type)) {
            StagingStorage tokenStorage = bag.getTokenStorage();
            tokenStorage.addFixity(fixity);
            fixity.setStorage(tokenStorage);
        } else if (BAG_TYPE.equalsIgnoreCase(type)) {
            StagingStorage bagStorage = bag.getBagStorage();
            bagStorage.addFixity(fixity);
            fixity.setStorage(bagStorage);
        } else {
            // hmmm
            // we'll probably want to validate the response body as well so maybe create this off the bat
            response = ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        bagService.save(bag);
        return response;
    }

    /**
     * Retrieve a fixity for a bag identified by its algorithm
     *
     * todo: the db should really be pulling the fixity object out; when we look into
     *       using more of the querydsl functionality we should be able to do that
     *
     * @param id The id of the bag
     * @param type The type of storage to retrieve
     * @param algorithm The algorithm used to compute the fixity
     * @return The fixity value for the algorithm, if it exists
     */
    @GetMapping("/storage/{type}/fixity/{alg}")
    private Optional<Fixity> getFixity(@PathVariable("id") Long id, @PathVariable("type") String type, @PathVariable("alg") String algorithm) {
        access.info("[GET /api/bags/{}/storage/{}/fixity/{alg}]", id, type, algorithm);

        BagSearchCriteria criteria = new BagSearchCriteria().withId(id);
        Bag bag = bagService.find(criteria);
        return bag.getBagStorage().getFixities()
                .stream()
                .filter(fixity -> fixity.getAlgorithm().equals(algorithm))
                .findFirst();

    }

}
