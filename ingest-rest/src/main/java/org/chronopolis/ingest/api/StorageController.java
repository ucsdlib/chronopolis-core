package org.chronopolis.ingest.api;

import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.models.RegionCreate;
import org.chronopolis.ingest.repository.StorageRegionRepository;
import org.chronopolis.ingest.repository.criteria.StorageRegionSearchCriteria;
import org.chronopolis.ingest.repository.dao.SearchService;
import org.chronopolis.rest.entities.storage.ReplicationConfig;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

/**
 * API methods to query StorageRegions
 *
 * Created by shake on 7/11/17.
 */
@RestController
@RequestMapping("/api/storage")
public class StorageController extends IngestController {

    private SearchService<StorageRegion, Long, StorageRegionRepository> service;

    @Autowired
    public StorageController(SearchService<StorageRegion, Long, StorageRegionRepository> service) {
        this.service = service;
    }

    /**
     * Retrieve a StorageRegion by its id
     *
     * @param id the id of the StorageRegion
     * @return the StorageRegion
     */
    @GetMapping("{id}")
    public StorageRegion getRegion(@PathVariable("id") Long id) {
        StorageRegionSearchCriteria criteria = new StorageRegionSearchCriteria();
        criteria.withId(id);
        return service.find(criteria);
    }

    /**
     * Retrieve all StorageRegions
     *
     * todo: query params (possibly a ModelAttr?)
     *
     * @return all StorageRegions
     */
    @GetMapping
    public Page<StorageRegion> getRegions() {
        StorageRegionSearchCriteria criteria = new StorageRegionSearchCriteria();
        return service.findAll(criteria, createPageRequest(new HashMap<>(), new HashMap<>()));
    }

    /**
     * Create a StorageRegion for a node
     *
     * todo: get the node owning the region
     * todo: validate that one can create for the node
     *
     * @param create the request containing the information about the SR
     * @return the newly created StorageRegion
     */
    @PostMapping
    public StorageRegion createRegion(@RequestBody RegionCreate create) {
        StorageRegion region = new StorageRegion();
        region.setCapacity(create.getCapacity())
                .setType(create.getType())
                // Not sure if this will work but hopefully it does
                .setReplicationConfig(new ReplicationConfig()
                                .setPath(create.getReplicationPath())
                                .setServer(create.getReplicationServer())
                                .setUsername(create.getReplicationUser()));
        service.save(region);
        return region;
    }


}
