package org.chronopolis.ingest.api;

import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.models.filter.StorageRegionFilter;
import org.chronopolis.ingest.repository.dao.PagedDao;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.QNode;
import org.chronopolis.rest.entities.storage.QStorageRegion;
import org.chronopolis.rest.entities.storage.ReplicationConfig;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.create.RegionCreate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Collections;

/**
 * API methods to query StorageRegions
 * <p>
 * Created by shake on 7/11/17.
 */
@RestController
@RequestMapping("/api/storage")
public class StorageController extends IngestController {

    private final PagedDao dao;

    @Autowired
    public StorageController(PagedDao dao) {
        this.dao = dao;
    }

    /**
     * Retrieve a StorageRegion by its id
     *
     * @param id the id of the StorageRegion
     * @return the StorageRegion
     */
    @GetMapping("{id}")
    public StorageRegion getRegion(@PathVariable("id") Long id) {
        return dao.findOne(QStorageRegion.storageRegion, QStorageRegion.storageRegion.id.eq(id));
    }

    /**
     * Retrieve all StorageRegions
     *
     * @param filter The query parameters to filter on
     * @return all StorageRegions
     */
    @GetMapping
    public Page<StorageRegion> getRegions(@ModelAttribute StorageRegionFilter filter) {
        return dao.findPage(QStorageRegion.storageRegion, filter);
    }

    /**
     * Create a StorageRegion for a node
     * <p>
     * todo: some type of identifier (local??) for storage regions?
     * should this be included in the create call?
     *
     * @param create the request containing the information about the SR
     * @return 201 with the new StorageRegion
     *         400 if the request is not valid
     *         403 if the user does not have permissions to create the StorageRegion
     */
    @PostMapping
    public ResponseEntity<StorageRegion> createRegion(Principal principal,
                                                      @RequestBody RegionCreate create) {
        ResponseEntity<StorageRegion> entity = ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .build();

        // Good enough I suppose
        if (hasRoleAdmin() || principal.getName().equalsIgnoreCase(create.getNode())) {
            Node node = dao.findOne(QNode.node, QNode.node.username.eq(create.getNode()));

            // check if the create exists, and if not return a bad request
            if (node == null) {
                entity = ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .build();
            } else {
                StorageRegion region = new StorageRegion();
                region.setCapacity(create.getCapacity());
                region.setNode(node);
                region.setNote(create.getNote());
                region.setDataType(create.getDataType());
                region.setStorageType(create.getStorageType());

                ReplicationConfig config = new ReplicationConfig(region,
                        create.getReplicationPath(),
                        create.getReplicationServer(),
                        create.getReplicationUser());
                region.setReplicationConfig(config);
                region.setStorage(Collections.emptySet());
                dao.save(region);
                entity = ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(region);
            }
        }

        return entity;
    }

}
