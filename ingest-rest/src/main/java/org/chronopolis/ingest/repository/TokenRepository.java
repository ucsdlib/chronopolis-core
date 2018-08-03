package org.chronopolis.ingest.repository;

import org.chronopolis.rest.entities.AceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

/**
 * Repository for interacting with {@link AceToken}s
 *
 *
 * Created by shake on 2/5/15.
 */
public interface TokenRepository extends JpaRepository<AceToken, Long> ,
                                         QueryDslPredicateExecutor<AceToken> {

    Long countByBagId(Long bagId);

}
