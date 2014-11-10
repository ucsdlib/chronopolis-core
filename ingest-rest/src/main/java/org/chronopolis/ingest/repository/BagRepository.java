package org.chronopolis.ingest.repository;

import org.chronopolis.ingest.model.Bag;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by shake on 11/6/14.
 */
public interface BagRepository extends CrudRepository<Bag, Long> {

    Bag findById(Long id);
}
