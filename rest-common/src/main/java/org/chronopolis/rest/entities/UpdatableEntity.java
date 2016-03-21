package org.chronopolis.rest.entities;

import org.chronopolis.rest.LocalDateTimeConverter;
import org.chronopolis.rest.listener.UpdatableEntityListener;

import javax.persistence.Convert;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.time.LocalDateTime;

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

    @Convert(converter = LocalDateTimeConverter.class)
    LocalDateTime createdAt;

    @Convert(converter = LocalDateTimeConverter.class)
    LocalDateTime updatedAt;


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public UpdatableEntity setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public UpdatableEntity setUpdatedAt(LocalDateTime updatedAt) {
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
