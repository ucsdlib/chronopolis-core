package org.chronopolis.rest.listener;

import org.chronopolis.rest.entities.UpdatableEntity;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import java.time.LocalDateTime;

/**
 * EntityListener which updates the created_at on the initial save,
 * and updated_at on all others
 *
 * Created by shake on 3/21/16.
 */
public class UpdatableEntityListener {

    @PrePersist
    public void createTimeStamps(UpdatableEntity ue) {
        ue.setCreatedAt(LocalDateTime.now());
        ue.setUpdatedAt(LocalDateTime.now());
    }

    @PreUpdate
    public void updateTimeStamps(UpdatableEntity ue) {
        ue.setUpdatedAt(LocalDateTime.now());
    }

}
