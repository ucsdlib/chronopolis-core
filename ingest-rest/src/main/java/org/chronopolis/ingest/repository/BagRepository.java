package org.chronopolis.ingest.repository;

import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

/**
 * Created by shake on 11/6/14.
 */
public interface BagRepository extends JpaRepository<Bag, Long> {

    // Bag findById(Long id);
    Bag findByNameAndDepositor(String name, String depositor);

    Collection<Bag> findByStatus(BagStatus status);

}
