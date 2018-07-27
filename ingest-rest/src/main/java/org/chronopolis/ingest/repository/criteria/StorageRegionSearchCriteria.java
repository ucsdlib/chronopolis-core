package org.chronopolis.ingest.repository.criteria;

import com.querydsl.core.types.dsl.BooleanExpression;
import org.chronopolis.ingest.api.Params;
import org.chronopolis.rest.kot.entities.storage.QStorageRegion;
import org.chronopolis.rest.kot.models.enums.DataType;
import org.chronopolis.rest.kot.models.enums.StorageType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Name is a bit verbose but that's ok...
 *
 * Created by shake on 7/11/17.
 */
public class StorageRegionSearchCriteria implements SearchCriteria {

    private final QStorageRegion region;
    private Map<Object, BooleanExpression> criteria;

    public StorageRegionSearchCriteria() {
        region = QStorageRegion.storageRegion;
        criteria = new HashMap<>();
    }

    public StorageRegionSearchCriteria withNodeName(List<String> names) {
        if (names != null && !names.isEmpty()) {
            criteria.put(Params.NODE, region.node.username.in(names));
        }
        return this;
    }

    public StorageRegionSearchCriteria withStorageType(StorageType type) {
        if (type != null) {
            criteria.put(Params.STORAGE_TYPE, region.storageType.eq(type));
        }
        return this;
    }

    public StorageRegionSearchCriteria withDataType(DataType type) {
        if (type != null) {
            criteria.put(Params.DATA_TYPE, region.dataType.eq(type));
        }

        return this;
    }

    public StorageRegionSearchCriteria withCapacityGreaterThan(Long capacity) {
        if (capacity != null) {
          criteria.put(Params.CAPACITY_GT, region.capacity.gt(capacity));
        }
        return this;
    }

    public StorageRegionSearchCriteria withCapacityLessThan(Long capacity) {
        if (capacity != null) {
          criteria.put(Params.CAPACITY_LT, region.capacity.lt(capacity));
        }
        return this;
    }

    @Override
    public Map<Object, BooleanExpression> getCriteria() {
        return criteria;
    }

    public StorageRegionSearchCriteria withId(Long id) {
        if (id != null) {
            criteria.put(Params.SORT_ID, region.id.eq(id));
        }
        return this;
    }
}
