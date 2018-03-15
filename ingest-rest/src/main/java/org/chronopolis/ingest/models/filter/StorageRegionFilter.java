package org.chronopolis.ingest.models.filter;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import org.chronopolis.ingest.models.Paged;
import org.chronopolis.rest.entities.storage.QStorageRegion;
import org.chronopolis.rest.models.storage.StorageType;

import java.util.List;

/**
 * Filter for queries on StorageRegions
 *
 * Created by shake on 7/12/17.
 */
public class StorageRegionFilter extends Paged {

    private final BooleanBuilder builder = new BooleanBuilder();
    private final QStorageRegion region = QStorageRegion.storageRegion;

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
        if (type != null) {
            this.type = type;
            builder.and(region.storageType.eq(type));
        }
        return this;
    }

    public List<String> getName() {
        return name;
    }

    public StorageRegionFilter setName(List<String> name) {
        if (name != null && !name.isEmpty()) {
            this.name = name;
            builder.and(region.node.username.in(name));
        }
        return this;
    }

    public Long getCapacityLess() {
        return capacityLess;
    }

    public StorageRegionFilter setCapacityLess(Long capacityLess) {
        if (capacityLess != null && capacityLess > 0) {
            this.capacityLess = capacityLess;
            builder.and(region.capacity.lt(capacityLess));
        }
        return this;
    }

    public Long getCapacityGreater() {
        return capacityGreater;
    }

    public StorageRegionFilter setCapacityGreater(Long capacityGreater) {
        if (capacityGreater != null) {
            this.capacityGreater = capacityGreater;
            builder.and(region.capacity.gt(capacityGreater));
        }
        return this;
    }

    @Override
    public BooleanBuilder getQuery() {
        return builder;
    }

    @Override
    public OrderSpecifier getOrderSpecifier() {
        Order dir = getDirection();
        OrderSpecifier orderSpecifier;

        switch (getOrderBy()) {
            case "capacity":
                orderSpecifier = new OrderSpecifier<>(dir, region.capacity);
                break;
            case "createdAt":
                orderSpecifier = new OrderSpecifier<>(dir, region.createdAt);
                break;
            case "updatedAt":
                orderSpecifier = new OrderSpecifier<>(dir, region.updatedAt);
                break;
            default:
                orderSpecifier = new OrderSpecifier<>(dir, region.id);
                break;
        }

        return orderSpecifier;
    }
}
