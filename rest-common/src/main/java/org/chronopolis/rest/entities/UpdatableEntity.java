package org.chronopolis.rest.entities;

import org.chronopolis.rest.ZonedDateTimeConverter;
import org.chronopolis.rest.listener.UpdatableEntityListener;

import javax.persistence.Convert;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.time.ZonedDateTime;
import java.time.ZonedDateTime;

/**
 * Entity which can be updated, and has the latest changes tracked by timestamp
 *
 * Created by shake on 3/21/16.
 */
@MappedSuperclass
@EntityListeners(UpdatableEntityListener.class)
public class UpdatableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Convert(converter = ZonedDateTimeConverter.class)
    ZonedDateTime createdAt;

    @Convert(converter = ZonedDateTimeConverter.class)
    ZonedDateTime updatedAt;


    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public UpdatableEntity setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public UpdatableEntity setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public Long getId() {
        return id;
    }

    public UpdatableEntity setId(Long id) {
        this.id = id;
        return this;
    }
}
