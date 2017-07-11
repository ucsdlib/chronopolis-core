package org.chronopolis.ingest.repository.criteria;

import com.querydsl.core.types.dsl.BooleanExpression;
import org.chronopolis.ingest.api.Params;
import org.chronopolis.rest.entities.storage.QStorage;

import java.util.HashMap;
import java.util.Map;

/**
 * SearchCriteria for our ummmm Storage entity
 *
 * Created by shake on 7/11/17.
 */
public class StorageSearchCriteria implements SearchCriteria {
    private final QStorage storage;
    private Map<Object, BooleanExpression> criteria;

    public StorageSearchCriteria() {
        storage = QStorage.storage;
        criteria = new HashMap<>();
    }

    public StorageSearchCriteria withNodeName(String name) {
        if (name != null) {
            criteria.put(Params.NODE, storage.region.node.username.eq(name));
        }
        return this;
    }

    public StorageSearchCriteria withRegion(Long id) {
        if (id != null) {
            criteria.put(Params.SORT_ID, storage.region.id.eq(id));
        }
        return this;
    }

    public StorageSearchCriteria withChecksum(String checksum) {
        if (checksum != null) {
            criteria.put(Params.CHECKSUM, storage.checksum.eq(checksum));
        }
        return this;
    }

    @Override
    public Map<Object, BooleanExpression> getCriteria() {
        return criteria;
    }
}
