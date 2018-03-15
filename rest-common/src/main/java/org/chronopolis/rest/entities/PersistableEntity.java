package org.chronopolis.rest.entities;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * Mark an Entity as Persistable and return the id
 *
 * @author shake
 */
@MappedSuperclass
public class PersistableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    public Long getId() {
        return id;
    }

    public PersistableEntity setId(Long id) {
        this.id = id;
        return this;
    }

}
