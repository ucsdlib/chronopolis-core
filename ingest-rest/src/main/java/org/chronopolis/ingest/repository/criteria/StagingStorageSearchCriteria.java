package org.chronopolis.ingest.repository.criteria;

import com.querydsl.core.types.dsl.BooleanExpression;
import org.chronopolis.ingest.api.Params;
import org.chronopolis.rest.entities.storage.QStagingStorage;

import java.util.HashMap;
import java.util.Map;

/**
 * SearchCriteria for our ummmm Storage entity
 *
 * Created by shake on 7/11/17.
 */
public class StagingStorageSearchCriteria implements SearchCriteria {
    private final QStagingStorage storage;
    private Map<Object, BooleanExpression> criteria;

    public StagingStorageSearchCriteria() {
        storage = QStagingStorage.stagingStorage;
        criteria = new HashMap<>();
    }

    public StagingStorageSearchCriteria withId(Long id) {
        if (id != null) {
            criteria.put(Params.SORT_ID, storage.id.eq(id));
        }
        return this;
    }

    public StagingStorageSearchCriteria withNodeName(String name) {
        if (name != null) {
            criteria.put(Params.NODE, storage.region.node.username.eq(name));
        }
        return this;
    }

    public StagingStorageSearchCriteria withRegion(Long id) {
        if (id != null) {
            criteria.put(Params.REGION, storage.region.id.eq(id));
        }
        return this;
    }

    @Override
    public Map<Object, BooleanExpression> getCriteria() {
        return criteria;
    }
}
