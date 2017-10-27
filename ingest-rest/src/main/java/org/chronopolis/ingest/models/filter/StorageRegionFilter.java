package org.chronopolis.ingest.models.filter;

import org.chronopolis.ingest.models.Paged;
import org.chronopolis.rest.models.storage.StorageType;

import java.util.List;

/**
 * Filter for queries on StorageRegions
 *
 * Created by shake on 7/12/17.
 */
public class StorageRegionFilter extends Paged {

    private Long id;
    private StorageType type;
    private List<String> name;
    private Long capacityLess;
    private Long capacityGreater;


    public Long getId() {
        return id;
    }

    public StorageRegionFilter setId(Long id) {
        this.id = id;
        return this;
    }

    public StorageType getType() {
        return type;
    }

    public StorageRegionFilter setType(StorageType type) {
        this.type = type;
        return this;
    }

    public List<String> getName() {
        return name;
    }

    public StorageRegionFilter setName(List<String> name) {
        this.name = name;
        return this;
    }

    public Long getCapacityLess() {
        return capacityLess;
    }

    public StorageRegionFilter setCapacityLess(Long capacityLess) {
        this.capacityLess = capacityLess;
        return this;
    }

    public Long getCapacityGreater() {
        return capacityGreater;
    }

    public StorageRegionFilter setCapacityGreater(Long capacityGreater) {
        this.capacityGreater = capacityGreater;
        return this;
    }
}
