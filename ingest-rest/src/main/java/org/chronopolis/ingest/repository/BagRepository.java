package org.chronopolis.ingest.repository;

import org.chronopolis.ingest.model.Bag;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by shake on 11/6/14.
 */
public interface BagRepository extends JpaRepository<Bag, Long> {

    // Bag findById(Long id);
    Bag findByNameAndDepositor(String name, String depositor);

}
