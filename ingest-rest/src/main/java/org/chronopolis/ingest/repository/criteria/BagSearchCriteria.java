package org.chronopolis.ingest.repository.criteria;

import com.querydsl.core.types.dsl.BooleanExpression;
import org.chronopolis.ingest.api.Params;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.models.BagStatus;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Search criteria that map to the query parameters one may pass in when getting bags
 *
 * Created by shake on 5/20/15.
 */
public class BagSearchCriteria implements SearchCriteria {
    private final QBag bag;

    // TODO: We could do a multimap in order to get OR relations
    private Map<Object, BooleanExpression> criteria;

    public BagSearchCriteria() {
        this.bag = QBag.bag;
        this.criteria = new HashMap<>();
    }

    public BagSearchCriteria withId(Long id) {
        if (id != null) {
            criteria.put(Params.SORT_ID, bag.id.eq(id));
        }

        return this;
    }

    public BagSearchCriteria withName(String name) {
        if (name != null && !name.isEmpty()) {
            criteria.put(Params.NAME, bag.name.eq(name));
        }
        return this;
    }

    public BagSearchCriteria nameLike(String name) {
        if (name != null && !name.isEmpty()) {
            criteria.put(Params.NAME, bag.name.like("%" + name + "%"));
        }
        return this;
    }

    public BagSearchCriteria withDepositor(String depositor) {
        if (depositor != null && !depositor.isEmpty()) {
            criteria.put(Params.DEPOSITOR, bag.depositor.eq(depositor));
        }
        return this;
    }

    public BagSearchCriteria withRegion(String region) {
        if (region != null) {
            Long regionId = Long.parseLong(region);
            criteria.put(Params.REGION, bag.bagStorage.region.id.eq(regionId));
        }
        return this;
    }

    public BagSearchCriteria withActiveStorage(String active) {
        if (active != null) {
            Boolean isActive = Boolean.parseBoolean(active);
            criteria.put(Params.ACTIVE, bag.bagStorage.active.eq(isActive));
        }
        return this;
    }

    public BagSearchCriteria depositorLike(String depositor) {
        if (depositor != null && !depositor.isEmpty()) {
            criteria.put(Params.DEPOSITOR, bag.depositor.like("%" + depositor + "%"));
        }
        return this;
    }

    public BagSearchCriteria withStatus(BagStatus status) {
        if (status != null) {
            criteria.put(Params.STATUS, bag.status.eq(status));
        }
        return this;
    }

    public BagSearchCriteria withStatuses(List<BagStatus> statuses) {
        if (statuses != null) {
            criteria.put(Params.STATUS, bag.status.in(statuses));
        }

        return this;
    }

    public BagSearchCriteria updatedAfter(String datetime) {
        if (datetime != null) {
            criteria.put("UPDATED_AFTER", bag.updatedAt.after(ZonedDateTime.parse(datetime)));
        }
        return this;
    }

    public BagSearchCriteria updatedBefore(String datetime) {
        if (datetime != null) {
            criteria.put("UPDATED_BEFORE", bag.updatedAt.before(ZonedDateTime.parse(datetime)));
        }
        return this;
    }

    public BagSearchCriteria createdAfter(String datetime) {
        if (datetime != null) {
            criteria.put("CREATED_AFTER", bag.createdAt.after(ZonedDateTime.parse(datetime)));
        }
        return this;
    }

    public BagSearchCriteria createdBefore(String datetime) {
        if (datetime != null) {
            criteria.put("CREATED_AFTER", bag.createdAt.before(ZonedDateTime.parse(datetime)));
        }
        return this;
    }

    public Map<Object, BooleanExpression> getCriteria() {
        return criteria;
    }
}
