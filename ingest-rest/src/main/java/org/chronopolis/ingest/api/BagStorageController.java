package org.chronopolis.ingest.api;

import org.chronopolis.ingest.repository.dao.StagingDao;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.models.create.StagingCreate;
import org.chronopolis.rest.models.update.ActiveToggle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Optional;

import static org.chronopolis.ingest.IngestController.hasRoleAdmin;
import static org.chronopolis.ingest.repository.dao.StagingDao.DISCRIMINATOR_BAG;
import static org.chronopolis.ingest.repository.dao.StagingDao.DISCRIMINATOR_TOKEN;

/**
 * REST controller for interacting with the Storage fields in a Bag
 * <p>
 *
 * @author shake
 */
@RestController
@RequestMapping("/api/bags/{id}")
public class BagStorageController {

    private final StagingDao dao;

    @Autowired
    public BagStorageController(StagingDao stagingDao) {
        this.dao = stagingDao;
    }


    /**
     * Retrieve the active storage area a bag resides in
     *
     * @param id   The id of the bag
     * @param type The type of storage to retrieve
     * @return The bag's storage information
     */
    @GetMapping("/storage/{type}")
    private ResponseEntity<StagingStorage> getBagStorage(@PathVariable("id") Long id,
                                                         @PathVariable("type") String type) {
        return storageFor(id, type)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a {@link StagingStorage} resource for a {@link Bag} if one does not already exist
     *
     * @param principal the security principal of the user
     * @param id        the id of the {@link Bag}
     * @param type      the type of storage to retrieve
     * @return 200 if the operation was successful
     *         400 if a bag does not exist or the type does not match a known discriminator value
     *         403 if the user is not allowed to update this resource
     *         409 if an active {@link StagingStorage} resource already exists
     */
    @PutMapping("/storage/{type}")
    private ResponseEntity<StagingStorage> addStorage(Principal principal,
                                                      @PathVariable("id") Long id,
                                                      @PathVariable("type") String type,
                                                      @RequestBody StagingCreate create) {
        return dao.createStaging(principal, id, type, create);
    }

    /**
     * Update the activity of a bag storage area
     *
     * @param principal The security principal of the user
     * @param id        The id of the bag
     * @param type      The type of storage to retrieve
     * @param toggle    The active flag to set
     * @return 200 if the operation was successful
     *         400 if no active StagingStorage is available
     *         403 if the user is not allowed to update this resource
     */
    @PutMapping("/storage/{type}/active")
    private ResponseEntity<StagingStorage> updateStorage(Principal principal,
                                                         @PathVariable("id") Long id,
                                                         @PathVariable("type") String type,
                                                         @RequestBody ActiveToggle toggle) {

        Optional<StagingStorage> stagingStorage = storageFor(id, type);
        return stagingStorage.map(storage -> toggleStorage(storage, toggle, principal))
                .orElse(ResponseEntity.badRequest().build());
    }

    /**
     * Toggle the active storage for a given StagingStorage entity based on an ActiveToggle
     *
     * @param storage   the StagingStorage entity to toggle
     * @param toggle    the toggle
     * @param principal the security principal of the user
     * @return a ResponseEntity corresponding to the status of the operation
     */
    private ResponseEntity<StagingStorage> toggleStorage(StagingStorage storage,
                                                         ActiveToggle toggle,
                                                         Principal principal) {
        ResponseEntity<StagingStorage> response;
        String username = storage.getRegion().getNode().getUsername();
        if (hasRoleAdmin() || username.equalsIgnoreCase(principal.getName())) {
            storage.setActive(toggle.isActive());
            dao.save(storage);
            response = ResponseEntity.ok(storage);
        } else {
            response = ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return response;
    }

    /**
     * Retrieve the StagingStorage for a given Bag and type, or Optional.empty if it does not exist
     * <p>
     *
     * @param bag  the bag to retrieve the StagingStorage object from
     * @param type the type of StagingStorage to retrieve
     * @return the StagingStorage
     */
    private Optional<StagingStorage> storageFor(Long bag, String type) {
        Optional<StagingStorage> storage = Optional.empty();

        if (DISCRIMINATOR_TOKEN.equalsIgnoreCase(type)
                || DISCRIMINATOR_BAG.equalsIgnoreCase(type)) {
            storage = dao.activeStorageForBag(bag, type);
        }

        return storage;
    }

}
