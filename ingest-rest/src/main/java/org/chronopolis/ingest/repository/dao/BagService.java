package org.chronopolis.ingest.repository.dao;

import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.rest.entities.Bag;

import javax.transaction.Transactional;

/**
 * Service to build queries for bags from the search criteria
 *
 * Created by shake on 5/20/15.
 */
@Transactional
public class BagService extends SearchService<Bag, Long, BagRepository> {

    public BagService(BagRepository bagRepository) {
        super(bagRepository);
    }

}
