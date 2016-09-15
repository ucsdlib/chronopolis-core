package org.chronopolis.ingest.repository;

import org.chronopolis.rest.entities.AceToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

import java.util.List;

/**
 * Repository for interacting with {@link AceToken}s
 *
 *
 * Created by shake on 2/5/15.
 */
public interface TokenRepository extends JpaRepository<AceToken, Long> ,
                                         QueryDslPredicateExecutor<AceToken> {

    // TODO: Let's find a way to use a cursor instead of individual queries
    //       fetch time is sloooooooow for large page sizes
    List<AceToken> findByBagIdOrderByIdAsc(Long bagId);
    Page<AceToken> findByBagIdOrderByIdAsc(Long bagId, Pageable pable);

    Long countByBagId(Long bagId);

}
